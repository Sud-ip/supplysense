import { useState } from 'react'
import { salesApi } from './salesApi'
import type { CsvImportBatch } from '../../api/types'

const statusColors: Record<CsvImportBatch['status'], string> = {
  PROCESSING: 'text-ink/50',
  COMPLETED: 'text-success',
  COMPLETED_WITH_ERRORS: 'text-warning',
  FAILED: 'text-danger'
}

export default function ImportSalesPage() {
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] = useState<CsvImportBatch | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)

  async function handleUpload() {
    if (!file) return
    setError(null); setResult(null); setUploading(true)
    try {
      const batch = await salesApi.importCsv(file)
      setResult(batch)
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Import failed')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="font-display text-2xl font-semibold text-ink mb-1">Import sales from CSV</h1>
      <p className="text-ink/50 text-sm mb-6">
        Required columns, in a header row: <code className="text-xs bg-paper px-1.5 py-0.5 rounded data">sku</code>,{' '}
        <code className="text-xs bg-paper px-1.5 py-0.5 rounded data">locationName</code>,{' '}
        <code className="text-xs bg-paper px-1.5 py-0.5 rounded data">quantity</code>,{' '}
        <code className="text-xs bg-paper px-1.5 py-0.5 rounded data">unitPrice</code> (optional),{' '}
        <code className="text-xs bg-paper px-1.5 py-0.5 rounded data">soldAt</code> (optional, ISO-8601). A file can
        partly succeed - rows with problems are reported individually below.
      </p>

      <div className="bg-white border border-line rounded p-5 mb-5">
        {error && <div className="text-sm text-danger bg-danger-soft rounded px-3 py-2 mb-3">{error}</div>}
        <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="text-sm mb-4 block" />
        <button onClick={handleUpload} disabled={!file || uploading}
          className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5 disabled:opacity-50">
          {uploading ? 'Uploading...' : 'Upload and import'}
        </button>
      </div>

      {result && (
        <div className="bg-white border border-line rounded p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-medium text-ink">{result.filename}</h2>
            <span className={`text-sm font-medium ${statusColors[result.status]}`}>{result.status}</span>
          </div>

          <div className="grid grid-cols-3 gap-3 text-center mb-5">
            <div className="bg-paper rounded p-3">
              <div className="text-lg font-semibold text-ink data">{result.rowCount}</div>
              <div className="text-xs text-ink/50">Total rows</div>
            </div>
            <div className="bg-success-soft rounded p-3">
              <div className="text-lg font-semibold text-success data">{result.successCount}</div>
              <div className="text-xs text-ink/50">Succeeded</div>
            </div>
            <div className="bg-danger-soft rounded p-3">
              <div className="text-lg font-semibold text-danger data">{result.errorCount}</div>
              <div className="text-xs text-ink/50">Failed</div>
            </div>
          </div>

          {result.errors.length > 0 && (
            <div>
              <h3 className="text-sm font-medium text-ink mb-2">Row errors</h3>
              <ul className="text-sm space-y-1">
                {result.errors.map((e, i) => (
                  <li key={i} className="text-danger">Row {e.rowNumber}: {e.message}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
