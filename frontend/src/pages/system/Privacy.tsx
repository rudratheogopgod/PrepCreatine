import PageWrapper from '../../components/layout/PageWrapper'

export default function Privacy() {
  return (
    <PageWrapper maxWidth="max-w-3xl">
      <div className="prose dark:prose-invert">
        <h1>Privacy Policy</h1>
        <p>Last updated: {new Date().toLocaleDateString()}</p>
        <p>This is a placeholder for the PrepCreatine Privacy Policy.</p>
      </div>
    </PageWrapper>
  )
}
