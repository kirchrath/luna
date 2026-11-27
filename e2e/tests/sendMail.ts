import { createConnection } from 'node:net';

import { MAIL_ADDRESS, SMTP_PORT } from '../environment/settings.ts';

const SENDER = 'billing@power.test';

/** Sends a plain text email to the test mailbox with a minimal SMTP conversation (no client library needed). */
export async function sendMail(subject: string, text: string): Promise<void> {
  const message = [
    `From: Power company <${SENDER}>`,
    `To: ${MAIL_ADDRESS}`,
    `Subject: ${subject}`,
    'Content-Type: text/plain; charset=UTF-8',
    '',
    text,
  ].join('\r\n');
  const commands = [
    'HELO e2e',
    `MAIL FROM:<${SENDER}>`,
    `RCPT TO:<${MAIL_ADDRESS}>`,
    'DATA',
    `${message}\r\n.`,
    'QUIT',
  ];
  const socket = createConnection({ host: 'localhost', port: SMTP_PORT });
  socket.setEncoding('utf8');
  await new Promise<void>((resolve, reject) => {
    socket.on('error', reject);
    socket.on('close', () => {
      resolve();
    });
    // The server answers the greeting and every command with one line; the next command follows each answer.
    socket.on('data', (answer: string) => {
      if (/^[45]\d\d/m.test(answer)) {
        reject(new Error(`SMTP error: ${answer}`));
        socket.destroy();
        return;
      }
      const command = commands.shift();
      if (command !== undefined) {
        socket.write(`${command}\r\n`);
      }
    });
  });
}
