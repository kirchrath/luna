package de.kirchrath.luna.mail.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

class MailSchedulerTest {

    @Test
    void thatPollingShouldResultIntoOnePollPerAccount() {
        // given
        var applicationService = mock(MailApplicationService.class);
        when(applicationService.findAccountNames()).thenReturn(List.of("private", "work"));
        var scheduler = new MailScheduler(applicationService);

        // when
        scheduler.pollAccounts();

        // then
        verify(applicationService).poll("private");
        verify(applicationService).poll("work");
    }
}
