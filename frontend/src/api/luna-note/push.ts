import { createPushConnector, type PushConnector, type TokenSource } from '../shared';
import type { NoteChangedDto } from './types';

/** Topic of the push messages of luna-note (see luna-note/README.md). */
export const NOTES_TOPIC = '/topic/notes';

export type LunaNotePushConnector = PushConnector<NoteChangedDto>;

export function createLunaNotePushConnector(baseUrl: string, tokenSource: TokenSource): LunaNotePushConnector {
  return createPushConnector<NoteChangedDto>(baseUrl, NOTES_TOPIC, tokenSource);
}
