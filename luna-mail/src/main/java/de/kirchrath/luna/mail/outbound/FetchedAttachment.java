package de.kirchrath.luna.mail.outbound;

public record FetchedAttachment(String name, String contentType, Long size) {
}
