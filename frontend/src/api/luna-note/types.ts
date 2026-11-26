import type { components } from './schema';

type Schemas = components['schemas'];

export type NoteDto = Schemas['NoteDto'];
export type NoteDataDto = Schemas['NoteDataDto'];
export type NoteListDto = Schemas['NoteListDto'];
export type NoteListItemDto = Schemas['NoteListItemDto'];
export type NoteUpdateDto = Schemas['NoteUpdateDto'];
export type NoteChangedDto = Schemas['NoteChangedDto'];
export type NoteStatusDto = NoteDataDto['status'];
