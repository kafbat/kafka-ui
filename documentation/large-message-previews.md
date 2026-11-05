# Large message previews

Large messages use bounded previews to protect the browser from expensive
rendering. Expand a row to inspect its key, value, headers, and metadata, or
download the original content.

## Inspecting content

Oversized records show **Preview truncated**. Expanded keys and values above the
preview limit appear as plain text rather than in the formatted editor.
**Show next chunk** replaces the current chunk; **Previous chunk** returns to
the prior one. The warning shows the chunk number and preview range.

Key/Value/Headers selection and record metadata remain available. Large headers
show a bounded prefix instead of chunk navigation.

Opening a large record in the producer editor requires **Open anyway**
confirmation because editing the full content may make the tab unresponsive.
Cancelling leaves the editor closed. Bounded preview navigation does not require
confirmation.

## Copying and downloading

All copy and download actions use complete content, not the truncated preview.
They are available for normal messages as well as large ones.

| Action | Content | Filename |
| --- | --- | --- |
| **Download full message** | JSON record containing value, key, headers, partition, offset, and timestamp, regardless of the selected field | `<topic-name>-pN-oM.json` |
| **Download value/key/headers** | Only the selected field | `<topic-name>-pN-oM-value`, `-key`, or `-headers` |
| **Copy value/key/headers** | Only the selected field, copied to the clipboard | N/A |

Filenames use the actual topic name, partition (`N`), and offset (`M`).
Field-specific actions preserve keys and values as-is, without pretty-printing
JSON strings; headers are serialized as JSON.

JSON/CSV exports also use original data and are generated only when requested.

## Rendering limits and scope

The message table displays at most 512 UTF-16 code units of each key and value,
including tooltips. JSONPath previews are skipped for fields larger than
64 Ki (65,536) UTF-16 code units. Expanded previews use the same 64 Ki limit;
chunk boundaries avoid splitting surrogate pairs.

These are frontend rendering limits, not Kafka byte limits. API behavior,
configuration, polling, filters, and pagination are unchanged. Full records still
arrive through SSE and reside in browser memory; arbitrarily large records or
prolonged live consumption can still exhaust memory.
