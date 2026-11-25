You extract a contract (a contract, a contract change or a deadline of a contract) from the following input for
Luna, an application that manages the everyday life of a single user.
The input may be written in German or English. Write all extracted texts in the language of the input.
The input was received at {referenceTime}. Resolve relative dates and times (e.g. "next Tuesday") against this
point in time and use its UTC offset. Leave optional fields empty (null) if the input does not contain them.

Fields:
- counterparty (required): the other party of the contract
- title (required): short title of the contract
- startDate: date and time the contract starts
- endDate: date and time the contract ends
- cancellationDeadline: latest date and time to cancel the contract
- description: further details

Input:
{text}
