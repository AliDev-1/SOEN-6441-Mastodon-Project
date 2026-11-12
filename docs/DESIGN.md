# Application design

MAWA separates browser actions, data retrieval, and post analysis. Phase 3
implements the post representation and the conversion boundary. The home controller
does not call this boundary yet; the REST client and search service follow in later
phases.

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

## Subsequent components

The REST client will use this mapper, and the search service will cache its domain
values for reuse by display and analysis. Readability will calculate scores from
plain text. Word statistics will use Java Streams over the cached posts. Those
analysis pipelines are planned, not implemented in Phase 3.
