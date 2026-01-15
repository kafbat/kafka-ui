import React, { useState } from 'react';
import { Button } from 'components/common/Button/Button';
import ExportIcon from 'components/common/Icons/ExportIcon';

type CsvFetcher = () => Promise<string>;

interface DownloadCsvButtonProps {
  fetchCsv: CsvFetcher;
  filePrefix: string;
}

export function DownloadCsvButton({
  fetchCsv,
  filePrefix,
}: DownloadCsvButtonProps) {
  const [isDownloading, setIsDownloading] = useState(false);

  const handleDownload = async () => {
    setIsDownloading(true);
    try {
      const csvString = await fetchCsv();

      const blob = new Blob([csvString], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;

      const dateStr = new Date().toISOString().slice(0, 10);
      a.download = `${filePrefix}-${dateStr}.csv`;

      document.body.appendChild(a);
      a.click();

      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <Button
      disabled={isDownloading}
      buttonType="secondary"
      buttonSize="M"
      onClick={handleDownload}
    >
      {isDownloading ? (
        'Downloading...'
      ) : (
        <>
          <ExportIcon /> Export CSV
        </>
      )}
    </Button>
  );
}
