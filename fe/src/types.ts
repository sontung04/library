export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

export type ApiResponse = {
  ok: boolean;
  status: number;
  data: unknown;
};

export type Page = {
  title: string;
  html: string;
  setup?: () => void;
};
