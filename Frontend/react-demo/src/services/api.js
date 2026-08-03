import axios from 'axios'

const BASE_URL = '/api/resume'

export const analyzeResume = async (resumeFile, jobDescription) => {
  const formData = new FormData()
  formData.append('resume', resumeFile)
  formData.append('jobDescription', jobDescription)
  const response = await axios.post(`${BASE_URL}/analyze`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return response.data  // now returns { analysis, extractedText }
}

export const analyzeText = async (resumeText, jobDescription) => {
  const formData = new FormData()
  formData.append('resumeText', resumeText)
  formData.append('jobDescription', jobDescription)
  const response = await axios.post(`${BASE_URL}/analyze-text`, formData)
  return response.data
}

export const fixResume = async (resumeText, jobDescription, missingSkills, improvements) => {
  const response = await axios.post(`${BASE_URL}/fix`, {
    resumeText,
    jobDescription,
    missingSkills,
    improvements
  })
  return response.data
}

// Download: send as JSON, receive PDF blob
export const downloadResumePdf = async (resumeText, candidateName = '') => {
  const response = await axios.post(
    `${BASE_URL}/download-pdf`,
    { resumeText, candidateName },
    { responseType: 'blob', headers: { 'Content-Type': 'application/json' } }
  )
  const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', 'optimized-resume.pdf')
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}
