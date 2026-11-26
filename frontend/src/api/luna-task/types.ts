import type { components } from './schema';

type Schemas = components['schemas'];

export type TaskDto = Schemas['TaskDto'];
export type TaskDataDto = Schemas['TaskDataDto'];
export type TaskListDto = Schemas['TaskListDto'];
export type TaskListItemDto = Schemas['TaskListItemDto'];
export type TaskUpdateDto = Schemas['TaskUpdateDto'];
export type TaskChangedDto = Schemas['TaskChangedDto'];
export type TaskStatusDto = TaskDataDto['status'];
export type TaskPriorityDto = TaskDataDto['priority'];
