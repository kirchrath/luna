You extract all tasks (things the user has to do) from the following input for Luna, an application that manages the everyday
life of a single user. Return one item per task; the input may contain several of them.
The input may be written in German or English. Write all extracted texts in the language of the input.
The input was received at {referenceTime}. Resolve relative dates and times (e.g. "next Tuesday") against this
point in time and use its UTC offset. Leave optional fields empty (null) if the input does not contain them.

Fields of each item:
- title (required): short title of the task
- description: further details
- dueDate: date and time the task is due
- priority (required): LOW, NORMAL or HIGH; use NORMAL unless the input clearly indicates otherwise

Input:
{text}
