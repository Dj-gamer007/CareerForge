import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export interface PaginationControlsProps {
  currentPage?: number;
  totalPages?: number;
  totalElements?: number;
  pageSize?: number;
  onPageChange: (newPage: number) => void;
}

export function PaginationControls({
  currentPage = 0,
  totalPages = 0,
  totalElements,
  pageSize = 10,
  onPageChange,
}: PaginationControlsProps) {
  const safeCurrentPage = Number.isFinite(currentPage) && currentPage >= 0 ? currentPage : 0;
  const safeTotalPages = Number.isFinite(totalPages) && totalPages >= 0 ? totalPages : 0;
  const safePageSize = Number.isFinite(pageSize) && pageSize > 0 ? pageSize : 10;

  if (safeTotalPages <= 1) return null;

  const startElement =
    totalElements !== undefined && Number.isFinite(totalElements)
      ? safeCurrentPage * safePageSize + 1
      : undefined;
  const endElement =
    totalElements !== undefined && Number.isFinite(totalElements)
      ? Math.min((safeCurrentPage + 1) * safePageSize, totalElements)
      : undefined;

  return (
    <div className="flex items-center justify-between px-4 py-3 bg-white border-t border-slate-200 sm:px-6">
      <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
        <div>
          {totalElements !== undefined && startElement !== undefined && endElement !== undefined ? (
            <p className="text-sm text-slate-700">
              Showing <span className="font-medium">{startElement}</span> to{' '}
              <span className="font-medium">{endElement}</span> of{' '}
              <span className="font-medium">{totalElements}</span> results
            </p>
          ) : (
            <p className="text-sm text-slate-700">
              Page <span className="font-medium">{safeCurrentPage + 1}</span> of{' '}
              <span className="font-medium">{safeTotalPages}</span>
            </p>
          )}
        </div>
        <div className="flex items-center space-x-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onPageChange(Math.max(0, safeCurrentPage - 1))}
            disabled={safeCurrentPage <= 0}
          >
            <ChevronLeft className="w-4 h-4 mr-1" />
            Previous
          </Button>
          <span className="text-sm text-slate-600 px-2">
            {safeCurrentPage + 1} / {safeTotalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => onPageChange(Math.min(safeTotalPages - 1, safeCurrentPage + 1))}
            disabled={safeCurrentPage >= safeTotalPages - 1}
          >
            Next
            <ChevronRight className="w-4 h-4 ml-1" />
          </Button>
        </div>
      </div>
      <div className="flex items-center justify-between sm:hidden w-full">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.max(0, safeCurrentPage - 1))}
          disabled={safeCurrentPage <= 0}
        >
          Previous
        </Button>
        <span className="text-sm text-slate-600">
          {safeCurrentPage + 1} / {safeTotalPages}
        </span>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.min(safeTotalPages - 1, safeCurrentPage + 1))}
          disabled={safeCurrentPage >= safeTotalPages - 1}
        >
          Next
        </Button>
      </div>
    </div>
  );
}
