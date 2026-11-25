import type { components } from './schema';

type Schemas = components['schemas'];

export type ManualInputDataDto = Schemas['ManualInputDataDto'];
export type ManualInboundEventListDto = Schemas['ManualInboundEventListDto'];
export type ManualInboundEventListItemDto = Schemas['ManualInboundEventListItemDto'];
export type ElementReferenceDto = Schemas['ElementReferenceDto'];
export type InboundEventChangedDto = Schemas['InboundEventChangedDto'];
export type LunaElementTypeDto = ElementReferenceDto['type'];
export type InboundEventStatusDto = ManualInboundEventListItemDto['status'];
