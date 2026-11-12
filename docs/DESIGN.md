# Application design

MAWA separates browser actions, data retrieval, and post analysis. The current
implementation includes post representation, JSON conversion, and an asynchronous
REST client. The home controller does not fetch posts yet; the search service and
UI follow in later phases.

## Domain model

`Post` is a final Java class with final fields and getters. It stores the post ID,
author display name and account handle, publication `Instant`, language, readable
content, original link, and visibility. Null constructor values are rejected. IDs
remain opaque strings rather than numbers.

There are no JSON, Play, or HTTP dependencies in the model. A later streaming
adapter can produce the same post values without changing the analysis classes.
No actors or reactive streams are implemented in Delivery 1.

## Mapping boundary

`MastodonPostMapper.parse(String)` accepts a complete JSON timeline array and
returns an unmodifiable `List<Post>` in the API's order. Empty arrays are valid.
Malformed JSON, trailing JSON values, non-array bodies, and invalid entries throw
`IllegalArgumentException`; the future client/service will translate this into a
controlled application error. One bad entry rejects the response instead of
silently presenting incomplete analysis.

| Mastodon field | Model field | Mapping rule |
| --- | --- | --- |
| `id` | `id` | Required nonblank string; no numeric conversion |
| `account.acct` | `authorHandle` | Required nonblank string |
| `account.display_name` | `authorName` | Missing, null, or blank uses the handle |
| `created_at` | `createdAt` | Required ISO instant, including supported time offsets |
| `language` | `language` | Missing, null, or blank becomes `Unknown` |
| `content` | `content` | Required string; empty media-only content is valid; converted to text |
| `url`, falling back to `uri` | `url` | Missing, null, or blank URL uses URI; chosen link must be absolute HTTP(S) with a host |
| `visibility` | `visibility` | Required nonblank string; preserved for display |

Unexpected JSON types are not coerced into strings. Unused extra fields are ignored.
The selected fields and nullable URL/language behavior follow the official
[Mastodon Status documentation](https://docs.joinmastodon.org/entities/Status/).

The mapper handles top-level status content only. Attachments, custom emoji images,
nested boosts, and quoted-post expansion are outside the current display model.
It does not fetch URLs or impose a timeline request size; the REST client will
request at most 40 posts per page.

## Readable text

`HtmlText.toPlainText(String)` parses HTML locally with jsoup, removes script and
style elements, decodes character entities, and normalizes block boundaries and
whitespace into spaces. Unicode text and ordinary emoji are preserved. Image-only
content has no text; a null fragment returns an empty string.

Plain text is not trusted HTML: future Twirl templates must escape it normally.
For example, encoded angle brackets become literal text and must not be inserted
through Twirl's raw-HTML helpers. The converter follows jsoup's
[text extraction API](https://jsoup.org/apidocs/org/jsoup/nodes/Element.html#text()).

## Asynchronous REST client

`MastodonClient.fetchPosts(String hashtag, String maxId)` returns a
`CompletionStage<List<Post>>`. Play injects its managed `WSClient`, configuration,
and mapper into a singleton. The client sends one GET to
`/api/v1/timelines/tag/{encodedHashtag}` with `limit=40` and an optional `max_id`.
It makes no blocking waits or automatic retries. Cursor navigation and caching
remain responsibilities of later phases. The endpoint and cursor behavior follow
the [official timeline API](https://docs.joinmastodon.org/methods/timelines/).

The request uses an Authorization Bearer header, an Accept JSON header, and a
ten-second timeout. Only configured HTTPS origins are accepted, and redirects are
disabled to keep authorization on that origin. The origin cannot contain user
credentials, an API path, query, or fragment. Configuration is captured when the
client is created; changes require restarting the application.

Null or blank hashtags produce a failed stage with status 400. Full input rules
will be enforced by the search service. Nonempty tags are encoded as one path
segment; cursors are passed separately through the HTTP client's query API.

HTTP 200 responses are mapped through `MastodonPostMapper`. Empty arrays are valid;
malformed responses and responses exceeding 40 posts are rejected. Error response
bodies are not read. `MastodonException` carries controlled messages without raw
exceptions, response bodies, or credentials. Future controller actions will use
its status code to render an appropriate error page.

| Condition | Application status |
| --- | --- |
| Missing or blank hashtag | 400 |
| Missing or invalid instance/token configuration | 503 |
| Upstream 401 or 403 | 502 with an authentication explanation |
| Upstream 429 | 503 with a rate-limit explanation |
| Upstream 503 or a connection failure | 503 |
| Upstream 504 or a transport timeout | 504 |
| Other upstream status or malformed/oversized data | 502 |

Upstream authentication failures do not return 401 to the browser because the
credentials belong to the server configuration, not a browser login session.

## Subsequent components

The search service will cache the client's domain values for reuse by display
and analysis. Readability will calculate scores from
plain text. Word statistics will use Java Streams over the cached posts. Those
analysis pipelines are planned, not implemented in Phase 4.
