import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export interface PaginationControlsProps {
  currentPage: number;
  totalPages: number;
  totalElements?: number;
  pageSize?: number;
  onPageChange: (newPage: number) => void;
}

export function PaginationControls({
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
}: PaginationControlsProps) {
  if (totalPages <= 1) return null;

  const startElement = totalElements !== undefined && pageSize !== undefined ? currentPage * pageSize + 1 : undefined;
  const endElement =
    totalElements !== undefined && pageSize !== undefined
      ? Math.min((currentPage + 1) * pageSize, totalElements)
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
              Page <span className="font-medium">{currentPage + 1}</span> of{' '}
              <span className="font-medium">{totalPages}</span>
            </p>
          )}
        </div>
        <div className="flex items-center space-x-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 0}
          >
            <ChevronLeft className="w-4 h-4 mr-1" />
            Previous
          </Button>
          <span className="text-sm text-slate-600 px-2">
            {currentPage + 1} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
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
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 0}
        >
          Previous
        </Button>
        <span className="text-sm text-slate-600">
          {currentPage + 1} / {totalPages}
        </span>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
        >
          Next
        </Button>
      </div>
    </div>
  );
}
