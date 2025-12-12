import { ColumnDef } from '@tanstack/react-table';
import { FullConnectorInfo } from 'generated-sources';
import BreakableTextCell from 'components/common/NewTable/BreakableTextCell';
import { TagCell } from 'components/common/NewTable';

import { KafkaConnectLinkCell } from './cells/KafkaConnectLinkCell';
import TopicsCell from './cells/TopicsCell';
import RunningTasksCell, {
  getRunningTasksCountText,
} from './cells/RunningTasksCell';
import ActionsCell from './cells/ActionsCell';

export const connectorsColumns: ColumnDef<FullConnectorInfo, string>[] = [
  {
    header: 'Name',
    accessorKey: 'name',
    cell: KafkaConnectLinkCell,
    enableResizing: true,
  },
  {
    header: 'Connect',
    accessorKey: 'connect',
    cell: BreakableTextCell,
    filterFn: 'includesSome',
    meta: { filterVariant: 'multi-select' },
    enableResizing: true,
  },
  {
    header: 'Type',
    accessorKey: 'type',
    meta: { filterVariant: 'multi-select' },
    filterFn: 'arrIncludesSome',
    size: 120,
  },
  {
    header: 'Plugin',
    accessorKey: 'connectorClass',
    cell: BreakableTextCell,
    meta: { filterVariant: 'multi-select' },
    filterFn: 'arrIncludesSome',
    enableResizing: true,
  },
  {
    header: 'Topics',
    accessorKey: 'topics',
    cell: TopicsCell,
    enableColumnFilter: true,
    meta: {
      filterVariant: 'multi-select',
      csvFn: (row) => (row.topics ? row.topics.join(', ') : '-'),
    },
    filterFn: 'arrIncludesSome',
    enableResizing: true,
  },
  {
    header: 'Status',
    accessorKey: 'status.state',
    cell: TagCell,
    meta: { filterVariant: 'multi-select', csvFn: (row) => row.status.state },
    filterFn: 'arrIncludesSome',
  },
  {
    id: 'running_task',
    accessorKey: 'tasksCount',
    header: 'Running Tasks',
    cell: RunningTasksCell,
    size: 120,
    meta: { csvFn: (row) => getRunningTasksCountText(row).text },
  },
  {
    header: '',
    id: 'action',
    cell: ActionsCell,
    size: 60,
  },
];
