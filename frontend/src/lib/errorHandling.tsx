import React from 'react';
import Alert from 'components/common/Alert/Alert';
import toast, { ToastType } from 'react-hot-toast';
import { ErrorResponse } from 'generated-sources';

export interface ServerResponse {
  status?: number;
  statusText: string;
  url?: string;
  message?: ErrorResponse['message'];
}
export type ToastTypes = ToastType | 'warning';

export const getResponse = async (error: unknown): Promise<ServerResponse> => {
  if (error instanceof Response) {
    let body;

    try {
      body = await error.json();
    } catch {
      // do nothing
    }

    const apiError: ServerResponse = {
      status: error.status,
      statusText: error.statusText,
      url: error.url,
      message: body?.message,
    };

    return apiError;
  }

  return {
    statusText: 'Unknown error',
    message: error instanceof Error ? error.message : 'Unknown error',
  };
};

export const apiFetch = async <T,>(fn: () => Promise<T>): Promise<T> => {
  try {
    return await fn();
  } catch (e) {
    throw await getResponse(e);
  }
};

interface AlertOptions {
  id?: string;
  title?: string;
  message: React.ReactNode;
}

export const showAlert = (
  type: ToastTypes,
  { title, message, id }: AlertOptions
) => {
  toast.custom(
    (t) => (
      <Alert
        title={title || ''}
        type={type}
        message={message}
        onDissmiss={() => toast.remove(t.id)}
      />
    ),
    { id }
  );
};

export const showSuccessAlert = (options: AlertOptions) => {
  showAlert('success', {
    ...options,
    title: options.title || 'Success',
  });
};

export const showServerError = async (
  response: Response,
  options?: AlertOptions
) => {
  let body: Record<string, string> = {};
  try {
    body = await response.json();
  } catch (e) {
    // do nothing;
  }
  if (response.status) {
    showAlert('error', {
      id: response.url,
      title: `${response.status} ${response.statusText}`,
      message: body?.message || 'An error occurred',
      ...options,
    });
  } else {
    showAlert('error', {
      id: 'server-error',
      title: `Something went wrong`,
      message: 'An error occurred',
      ...options,
    });
  }
};
