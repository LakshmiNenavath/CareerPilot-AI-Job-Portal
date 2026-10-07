// CareerPilot AI - Main Client Interactions
document.addEventListener('DOMContentLoaded', () => {
    initResumeUploader();
});

function initResumeUploader() {
    const dropArea = document.getElementById('resumeDropArea');
    const fileInput = document.getElementById('resumeFileInput');
    const uploadForm = document.getElementById('resumeUploadForm');
    const pasteModal = document.getElementById('pasteModal');
    const openPasteBtn = document.getElementById('openPasteBtn');
    const closePasteBtn = document.getElementById('closePasteBtn');
    const submitPasteBtn = document.getElementById('submitPasteBtn');
    const resumeTextInput = document.getElementById('resumeTextInput');
    const uploadStatus = document.getElementById('uploadStatus');
    const statusText = document.getElementById('statusText');
    const statusSpinner = document.getElementById('statusSpinner');

    if (!dropArea || !fileInput) return;

    // Open/Close Paste Modal
    if (openPasteBtn && pasteModal) {
        openPasteBtn.addEventListener('click', () => {
            pasteModal.style.display = 'flex';
        });
    }

    if (closePasteBtn && pasteModal) {
        closePasteBtn.addEventListener('click', () => {
            pasteModal.style.display = 'none';
        });
    }

    if (pasteModal) {
        pasteModal.addEventListener('click', (e) => {
            if (e.target === pasteModal) pasteModal.style.display = 'none';
        });
    }

    // Drag & Drop events
    ['dragenter', 'dragover'].forEach(eventName => {
        dropArea.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropArea.classList.add('border-primary', 'bg-hover');
        }, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropArea.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropArea.classList.remove('border-primary', 'bg-hover');
        }, false);
    });

    dropArea.addEventListener('drop', (e) => {
        const dt = e.dataTransfer;
        const files = dt.files;
        if (files && files.length > 0) {
            fileInput.files = files;
            handleFileUpload(files[0]);
        }
    });

    fileInput.addEventListener('change', () => {
        if (fileInput.files && fileInput.files.length > 0) {
            handleFileUpload(fileInput.files[0]);
        }
    });

    // Handle File Upload via AJAX
    function handleFileUpload(file) {
        if (!file) return;

        // Check file size (max 25MB)
        if (file.size > 25 * 1024 * 1024) {
            showError("File size exceeds 25MB limit. Please upload a smaller file.");
            return;
        }

        showLoading("Scanning resume text, matching technical skills & detecting job role...");

        const formData = new FormData();
        formData.append("file", file);

        fetch("/api/resume/upload-file", {
            method: "POST",
            body: formData
        })
        .then(async response => {
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || "Failed to process resume.");
            }
            return data;
        })
        .then(data => {
            showSuccess(`Detected Role: ${data.detectedRole} (ATS Score: ${data.atsScore}/100). Redirecting to analysis...`);
            setTimeout(() => {
                window.location.href = data.redirectUrl;
            }, 1200);
        })
        .catch(err => {
            showError(err.message || "An error occurred while uploading your resume.");
        });
    }

    // Handle Text Paste Submission
    if (submitPasteBtn && resumeTextInput) {
        submitPasteBtn.addEventListener('click', () => {
            const text = resumeTextInput.value.trim();
            if (text.length < 50) {
                alert("Please paste a realistic resume containing at least 50 characters of technical experience or education.");
                return;
            }

            pasteModal.style.display = 'none';
            showLoading("Scanning pasted resume content, extracting tech stack & detecting role...");

            fetch("/api/resume/upload-text", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    text: text,
                    title: "Pasted Student Resume"
                })
            })
            .then(async response => {
                const data = await response.json();
                if (!response.ok) {
                    throw new Error(data.error || "Failed to process resume text.");
                }
                return data;
            })
            .then(data => {
                showSuccess(`Detected Role: ${data.detectedRole} (ATS Score: ${data.atsScore}/100). Redirecting to analysis...`);
                setTimeout(() => {
                    window.location.href = data.redirectUrl;
                }, 1200);
            })
            .catch(err => {
                showError(err.message || "An error occurred while processing your resume text.");
            });
        });
    }

    function showLoading(msg) {
        if (uploadStatus) uploadStatus.style.display = 'block';
        if (statusSpinner) statusSpinner.style.display = 'inline-block';
        if (statusText) {
            statusText.textContent = msg;
            statusText.className = "text-sub";
        }
        if (dropArea) dropArea.style.opacity = '0.4';
    }

    function showSuccess(msg) {
        if (statusSpinner) statusSpinner.style.display = 'none';
        if (statusText) {
            statusText.textContent = msg;
            statusText.className = "text-emerald";
        }
    }

    function showError(msg) {
        if (uploadStatus) uploadStatus.style.display = 'block';
        if (statusSpinner) statusSpinner.style.display = 'none';
        if (statusText) {
            statusText.textContent = "Error: " + msg;
            statusText.className = "text-rose";
        }
        if (dropArea) dropArea.style.opacity = '1';
    }
}
