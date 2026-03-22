import { useMemo } from 'react'
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  type Node,
  type Edge,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { RefreshCw, CheckCircle2, Circle, AlertTriangle, Book, Loader2 } from 'lucide-react'
import { toast } from 'react-hot-toast'
import PageWrapper from '../../components/layout/PageWrapper'
import Button from '../../components/ui/Button'
import client from '../../api/client'
import { ENDPOINTS } from '../../api/endpoints'
import { logger } from '../../utils/logger'

interface RoadmapData {
  nodes: Node[]
  edges: Edge[]
}

const CustomNode = ({ data }: { data: any }) => {
  return (
    <div className={`px-4 py-3 rounded-xl border-2 shadow-sm min-w-[200px] bg-white dark:bg-slate-800 transition-colors ${
      data.status === 'completed' ? 'border-green-400 dark:border-green-500' :
      data.status === 'in_progress' ? 'border-amber-400 dark:border-amber-500' :
      'border-gray-200 dark:border-slate-700'
    }`}>
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          {data.status === 'completed' && <CheckCircle2 size={16} className="text-green-500" />}
          {data.status === 'in_progress' && <Loader2 size={16} className="text-amber-500 animate-spin" />}
          {data.status === 'not_started' && <Circle size={16} className="text-gray-400" />}
          <span className="text-xs font-body font-bold text-gray-500 dark:text-slate-400 uppercase">{data.subject}</span>
        </div>
        {data.isWeakSpot && <AlertTriangle size={14} className="text-red-500" />}
      </div>
      <p className="text-sm font-heading font-semibold text-gray-900 dark:text-slate-100">{data.label}</p>
      {data.estimatedHours && (
        <p className="text-xs font-body text-gray-500 dark:text-slate-400 mt-1 flex items-center gap-1">
          <Book size={12} /> {data.estimatedHours}h estimated
        </p>
      )}
    </div>
  )
}

const nodeTypes = {
  custom: CustomNode,
}

export default function Roadmap() {
  const queryClient = useQueryClient()
  
  const { data, isLoading } = useQuery<RoadmapData>({
    queryKey: ['roadmap'],
    queryFn: async () => {
      const res = await client.get(ENDPOINTS.ROADMAP)
      return res.data
    },
    staleTime: 5 * 60 * 1000,
    placeholderData: {
      nodes: [
        { id: '1', type: 'custom', position: { x: 250, y: 0 }, data: { label: 'Mechanics', subject: 'Physics', status: 'completed', estimatedHours: 15 } },
        { id: '2', type: 'custom', position: { x: 250, y: 150 }, data: { label: 'Electromagnetism', subject: 'Physics', status: 'in_progress', isWeakSpot: true, estimatedHours: 20 } },
        { id: '3', type: 'custom', position: { x: 250, y: 300 }, data: { label: 'Modern Physics', subject: 'Physics', status: 'not_started', estimatedHours: 10 } },
      ],
      edges: [
        { id: 'e1-2', source: '1', target: '2', animated: true, style: { stroke: '#0EA5E9' } },
        { id: 'e2-3', source: '2', target: '3', animated: false, style: { stroke: '#94A3B8' } },
      ],
    },
  })

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])

  useMemo(() => {
    if (data) {
      // Initialize layout correctly or use fetched positions
      setNodes(data.nodes)
      setEdges(data.edges)
    }
  }, [data, setNodes, setEdges])

  const regenerateMutation = useMutation({
    mutationFn: async () => {
      logger.info('[Roadmap] Regenerating')
      const res = await client.post(ENDPOINTS.ROADMAP_REGENERATE)
      return res.data
    },
    onSuccess: (newData) => {
      queryClient.setQueryData(['roadmap'], newData)
      toast.success('Your roadmap has been updated!')
    },
    onError: () => toast.error('Failed to regenerate roadmap.'),
  })

  return (
    <PageWrapper className="h-full flex flex-col py-0 px-0 sm:px-0 lg:px-0 max-w-none">
      <div className="px-4 sm:px-6 py-4 border-b border-gray-100 dark:border-slate-800 flex items-center justify-between bg-white dark:bg-slate-900 z-10">
        <div>
          <h1 className="text-xl font-heading font-bold text-gray-900 dark:text-white">Dynamic Study Roadmap</h1>
          <p className="text-xs font-body text-gray-500 dark:text-slate-400 mt-1">AI-generated sequence based on prerequisites and your performance.</p>
        </div>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => regenerateMutation.mutate()}
          loading={regenerateMutation.isPending}
          disabled={isLoading}
        >
          <RefreshCw size={14} /> Regenerate
        </Button>
      </div>
      
      <div className="flex-1 w-full bg-gray-50/50 dark:bg-slate-950/50 relative">
        {isLoading ? (
          <div className="absolute inset-0 flex items-center justify-center">
            <Loader2 className="w-8 h-8 text-sky-500 animate-spin" />
          </div>
        ) : (
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            nodeTypes={nodeTypes}
            fitView
            attributionPosition="bottom-right"
            className="dark:bg-slate-950"
          >
            <Background gap={16} size={1} />
            <MiniMap 
              nodeColor={(n) => {
                if (n.data?.status === 'completed') return '#22C55E'
                if (n.data?.status === 'in_progress') return '#F59E0B'
                return '#94A3B8'
              }}
              maskColor="rgba(0, 0, 0, 0.1)"
              className="dark:bg-slate-800 dark:mask-[#00000033]"
            />
            <Controls className="dark:bg-slate-800 dark:border-slate-700 dark:fill-slate-300" />
          </ReactFlow>
        )}
      </div>
    </PageWrapper>
  )
}
