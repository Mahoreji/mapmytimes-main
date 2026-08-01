export interface APIResponse<T> {
  success: boolean;
  statusCode: number;
  message: string;
  data: T;
  errors: string[];
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export type PaginationParams = {
  page?: number;
  size?: number;
  sort?: string;
};

export type PostStatus = "DRAFT" | "PUBLISHED" | "SCHEDULED" | "ARCHIVED" | "REJECTED";
export type PostType   = "BLOG" | "SOCIAL" | "STORY";
export type Visibility = "PUBLIC" | "PRIVATE" | "UNLISTED";

export type ID = string;
