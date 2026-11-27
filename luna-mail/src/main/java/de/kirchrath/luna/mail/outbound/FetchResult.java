package de.kirchrath.luna.mail.outbound;

import java.util.List;

/**
 * @param uidValidity UIDVALIDITY of the folder
 * @param lastUid     highest UID that is known now
 * @param emails      emails newer than the last known UID; none if the UIDVALIDITY was unknown or has changed
 */
public record FetchResult(long uidValidity, long lastUid, List<FetchedEmail> emails) {
}
