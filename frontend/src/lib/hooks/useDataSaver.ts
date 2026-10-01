import { showAlert, showSuccessAlert } from 'lib/errorHandling';

/**
 * Creates clipboard and file-saving actions for data supplied as a value or getter.
 */
const useDataSaver = (
  subject: string,
  data:
    | Record<string, string>
    | string
    | (() => Record<string, string> | string)
) => {
  const getData = () => (typeof data === 'function' ? data() : data);

  /** Copies the current data representation when clipboard access is available. */
  const copyToClipboard = () => {
    if (navigator.clipboard) {
      const currentData = getData();
      const str =
        typeof currentData === 'string'
          ? String(currentData)
          : JSON.stringify(currentData, null, '\t');
      navigator.clipboard.writeText(str);
      showSuccessAlert({
        id: subject,
        title: '',
        message: 'Copied successfully!',
      });
    } else {
      showAlert('warning', {
        id: subject,
        title: 'Warning',
        message:
          'Copying to clipboard is unavailable due to unsecured (non-HTTPS) connection',
      });
    }
  };
  /** Saves the current data as a browser-downloaded JSON file. */
  const saveFile = () => {
    const currentData = getData();
    const blob = new Blob([currentData as BlobPart], { type: 'text/json' });
    const elem = window.document.createElement('a');
    elem.href = window.URL.createObjectURL(blob);
    elem.download = subject;
    document.body.appendChild(elem);
    elem.click();
    document.body.removeChild(elem);
  };

  return { copyToClipboard, saveFile };
};

export default useDataSaver;
