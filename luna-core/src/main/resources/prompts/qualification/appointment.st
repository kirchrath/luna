You extract an appointment (a meeting or event at a specific date and time) from the following input for Luna, an
application that manages the everyday life of a single user.
The input may be written in German or English. Write all extracted texts in the language of the input.
The input was received at {referenceTime}. Resolve relative dates and times (e.g. "next Tuesday") against this
point in time and use its UTC offset. Leave optional fields empty (null) if the input does not contain them.

Fields:
- title (required): short title of the appointment
- start (required): start date and time; for all-day appointments the start of the day
- end: end date and time
- allDay (required): true if the appointment lasts the whole day, otherwise false
- location: where the appointment takes place
- description: further details

Input:
{text}
