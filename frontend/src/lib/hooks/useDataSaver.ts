import { showAlert, showSuccessAlert } from 'lib/errorHandling';

const useDataSaver = (
  subject: string,
  data:
    | Record<string, string>
    | string
    | (() => Record<string, string> | string)
) => {
  const getData = () => (typeof data === 'function' ? data() : data);
  const copyToClipboard = () => {
    if (navigator.clipboard) {
      const currentData = getData();
      const str =
        typeof currentData === 'string'
          ? currentData
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
  const saveFile = () => {
    const currentData = getData();
    const blob = new Blob([currentData as BlobPart], { type: 'text/json' });
    const elem = window.document.createElement('a');
    elem.href = window.URL.createObjectURL(blob);
    elem.download = subject;
    document.body.appendChild(elem);
    elem.click();
    document.body.removeChild(elem);
    window.URL.revokeObjectURL(elem.href);
  };

  return { copyToClipboard, saveFile };
};

export default useDataSaver;
