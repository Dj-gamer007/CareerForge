export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  page?: number;
  number?: number;
  first?: boolean;
  last?: boolean;
  numberOfElements?: number;
  empty?: boolean;
}

export interface ApiErrorResponse {
  success: false;
  message: string;
  timestamp?: string;
  errors?: Record<string, string>;
}
