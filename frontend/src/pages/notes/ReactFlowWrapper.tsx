import ReactFlow, {
  MiniMap, Controls, Background,
  useNodesState, useEdgesState,
  Node, Edge,
} from 'reactflow'
import 'reactflow/dist/style.css'

interface Props { sourceId: string }

const defaultNodes: Node[] = [
  { id: '1', position: { x: 200, y: 0 }, data: { label: 'Main Topic' }, style: { background: '#fff', border: '2px solid #00628c', borderRadius: 12, padding: '8px 16px', fontFamily: 'Manrope,sans-serif', fontWeight: 700, fontSize: 13, color: '#2c2f30' } },
  { id: '2', position: { x: 0, y: 100 }, data: { label: 'Sub-concept A' }, style: { background: '#f5f9fe', border: '1.5px solid #34b5fa', borderRadius: 10, padding: '6px 14px', fontFamily: 'Manrope,sans-serif', fontSize: 12, color: '#00628c' } },
  { id: '3', position: { x: 220, y: 100 }, data: { label: 'Sub-concept B' }, style: { background: '#f5f9fe', border: '1.5px solid #34b5fa', borderRadius: 10, padding: '6px 14px', fontFamily: 'Manrope,sans-serif', fontSize: 12, color: '#00628c' } },
  { id: '4', position: { x: 380, y: 100 }, data: { label: 'Sub-concept C' }, style: { background: '#f5f9fe', border: '1.5px solid #34b5fa', borderRadius: 10, padding: '6px 14px', fontFamily: 'Manrope,sans-serif', fontSize: 12, color: '#00628c' } },
  { id: '5', position: { x: 0, y: 200 }, data: { label: 'Key Formula' }, style: { background: '#eef2ff', border: '1.5px solid #584cb5', borderRadius: 10, padding: '6px 14px', fontFamily: 'Manrope,monospace', fontSize: 12, color: '#584cb5' } },
  { id: '6', position: { x: 220, y: 200 }, data: { label: 'Application' }, style: { background: '#f0fdf4', border: '1.5px solid #22c55e', borderRadius: 10, padding: '6px 14px', fontFamily: 'Manrope,sans-serif', fontSize: 12, color: '#166534' } },
]

const defaultEdges: Edge[] = [
  { id: 'e1-2', source: '1', target: '2', type: 'smoothstep', style: { stroke: '#34b5fa' } },
  { id: 'e1-3', source: '1', target: '3', type: 'smoothstep', style: { stroke: '#34b5fa' } },
  { id: 'e1-4', source: '1', target: '4', type: 'smoothstep', style: { stroke: '#34b5fa' } },
  { id: 'e2-5', source: '2', target: '5', type: 'smoothstep', style: { stroke: '#584cb5', strokeDasharray: '4' } },
  { id: 'e3-6', source: '3', target: '6', type: 'smoothstep', style: { stroke: '#22c55e', strokeDasharray: '4' } },
]

export default function ReactFlowWrapper({ sourceId }: Props) {
  const [nodes, , onNodesChange] = useNodesState(defaultNodes)
  const [edges, , onEdgesChange] = useEdgesState(defaultEdges)

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        fitView
        attributionPosition="bottom-right"
      >
        <Controls />
        <MiniMap />
        <Background gap={16} color="rgba(171,173,174,0.15)" />
      </ReactFlow>
    </div>
  )
}
