import type { components } from './schema';

type Schemas = components['schemas'];

export type ManualInputDataDto = Schemas['ManualInputDataDto'];
export type InboundEventListDto = Schemas['InboundEventListDto'];
export type InboundEventListItemDto = Schemas['InboundEventListItemDto'];
export type ElementReferenceDto = Schemas['ElementReferenceDto'];
export type InboundEventChangedDto = Schemas['InboundEventChangedDto'];
export type LunaElementTypeDto = ElementReferenceDto['type'];
export type InboundEventStatusDto = InboundEventListItemDto['status'];
export type InboundEventSourceDto = InboundEventListItemDto['source'];
export type LifeAreaDto = InboundEventListItemDto['area'];
