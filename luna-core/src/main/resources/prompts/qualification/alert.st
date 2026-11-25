You extract an alert (an important warning, e.g. about a system status or an undeliverable message) from the
following input for Luna, an application that manages the everyday life of a single user.
The input may be written in German or English. Write all extracted texts in the language of the input.
The input was received at {referenceTime}. Resolve relative dates and times (e.g. "next Tuesday") against this
point in time and use its UTC offset. Leave optional fields empty (null) if the input does not contain them.

Fields:
- title (required): short title of the alert
- message (required): what happened and why it matters
- severity (required): INFO, WARNING or CRITICAL
- affectedSystem: the affected system or service, e.g. a mail server

Input:
{text}
