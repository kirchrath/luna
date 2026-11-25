You extract all alerts (important warnings, e.g. about a system status or an undeliverable message) from the following input for Luna, an application that manages the everyday
life of a single user. Return one item per alert; the input may contain several of them.
The input may be written in German or English. Write all extracted texts in the language of the input.
The input was received at {referenceTime}. Resolve relative dates and times (e.g. "next Tuesday") against this
point in time and use its UTC offset. Leave optional fields empty (null) if the input does not contain them.

Fields of each item:
- title (required): short title of the alert
- message (required): what happened and why it matters
- severity (required): INFO, WARNING or CRITICAL
- affectedSystem: the affected system or service, e.g. a mail server

Input:
{text}
