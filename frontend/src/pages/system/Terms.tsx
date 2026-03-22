import PageWrapper from '../../components/layout/PageWrapper'

export default function Terms() {
  return (
    <PageWrapper maxWidth="max-w-3xl">
      <div className="prose dark:prose-invert">
        <h1>Terms of Service</h1>
        <p>Last updated: {new Date().toLocaleDateString()}</p>
        <p>This is a placeholder for the PrepCreatine Terms of Service.</p>
      </div>
    </PageWrapper>
  )
}
