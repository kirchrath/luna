You extract all invoices (bills or payment requests) from the following input for Luna, an application that manages the everyday
life of a single user. Return one item per invoice; the input may contain several of them.
The input may be written in German or English. Write all extracted texts in the language of the input.
The input was received at {referenceTime}. Resolve relative dates and times (e.g. "next Tuesday") against this
point in time and use its UTC offset. Leave optional fields empty (null) if the input does not contain them.

Fields of each item:
- issuer (required): who issued the invoice
- invoiceNumber: number of the invoice
- amount (required): amount to pay as a decimal number
- currency (required): ISO 4217 code, e.g. EUR
- dueDate: date and time the payment is due
- description: what the invoice is about

Input:
{text}
