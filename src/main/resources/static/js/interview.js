// CareerPilot AI - Interactive Mock Interview Suite
document.addEventListener('DOMContentLoaded', () => {
    initInterviewSuite();
});

function initInterviewSuite() {
    const candidateId = document.getElementById('interviewCandidateId')?.value;
    const timeLimitMinutes = parseInt(document.getElementById('interviewMinutes')?.value || "8", 10);
    
    // UI Elements
    const startScreen = document.getElementById('interviewStartScreen');
    const activeScreen = document.getElementById('interviewActiveScreen');
    const beginInterviewBtn = document.getElementById('beginInterviewBtn');
    
    const questionCategory = document.getElementById('questionCategory');
    const questionCounter = document.getElementById('questionCounter');
    const questionText = document.getElementById('questionText');
    const speakQuestionBtn = document.getElementById('speakQuestionBtn');
    
    const webcamVideo = document.getElementById('webcamVideo');
    const toggleWebcamBtn = document.getElementById('toggleWebcamBtn');
    const webcamStatus = document.getElementById('webcamStatus');
    
    const answerInput = document.getElementById('answerInput');
    const voiceRecordBtn = document.getElementById('voiceRecordBtn');
    const voiceRecordStatus = document.getElementById('voiceRecordStatus');
    const submitAnswerBtn = document.getElementById('submitAnswerBtn');
    const skipQuestionBtn = document.getElementById('skipQuestionBtn');
    
    const timerDisplay = document.getElementById('interviewTimerDisplay');
    const timerProgress = document.getElementById('interviewTimerProgress');
    
    const evalModal = document.getElementById('evalModal');
    const evalScore = document.getElementById('evalScore');
    const evalStrengths = document.getElementById('evalStrengths');
    const evalImprovements = document.getElementById('evalImprovements');
    const nextQuestionBtn = document.getElementById('nextQuestionBtn');

    if (!candidateId || !beginInterviewBtn) return;

    let sessionId = null;
    let questions = [];
    let currentQuestionIndex = 0;
    let timerInterval = null;
    let remainingSeconds = timeLimitMinutes * 60;
    const totalSeconds = remainingSeconds;
    
    let mediaStream = null;
    let recognition = null;
    let isRecording = false;

    // Web Speech API: Speech Recognition Setup
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
        recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.lang = 'en-US';

        recognition.onresult = (event) => {
            let transcript = '';
            for (let i = event.resultIndex; i < event.results.length; ++i) {
                transcript += event.results[i][0].transcript;
            }
            if (transcript.trim().length > 0) {
                const currentVal = answerInput.value;
                // If appending or updating
                answerInput.value = currentVal ? (currentVal.endsWith(' ') ? currentVal : currentVal + ' ') + transcript : transcript;
            }
        };

        recognition.onerror = (event) => {
            console.warn("Speech recognition error:", event.error);
            stopVoiceRecording();
        };

        recognition.onend = () => {
            if (isRecording) {
                // Restart if still marked as recording
                try { recognition.start(); } catch (e) {}
            }
        };
    } else {
        if (voiceRecordBtn) {
            voiceRecordBtn.title = "Speech recognition is not supported in this browser. Please type your answer.";
        }
    }

    // Toggle Voice Recording
    if (voiceRecordBtn) {
        voiceRecordBtn.addEventListener('click', () => {
            if (!recognition) {
                alert("Speech-to-text is not supported in this browser. Please use Chrome, Edge, or Safari, or type your response directly.");
                return;
            }
            if (!isRecording) {
                startVoiceRecording();
            } else {
                stopVoiceRecording();
            }
        });
    }

    function startVoiceRecording() {
        isRecording = true;
        try {
            recognition.start();
            voiceRecordBtn.classList.add('btn-danger');
            voiceRecordBtn.innerHTML = '<span class="pulsing-dot" style="background:#ef4444;box-shadow:0 0 8px #ef4444;"></span> Stop Recording';
            if (voiceRecordStatus) {
                voiceRecordStatus.textContent = "Listening... Speak clearly into your microphone.";
                voiceRecordStatus.style.color = "#34d399";
            }
        } catch (e) {
            console.error(e);
        }
    }

    function stopVoiceRecording() {
        isRecording = false;
        try {
            recognition.stop();
        } catch (e) {}
        if (voiceRecordBtn) {
            voiceRecordBtn.classList.remove('btn-danger');
            voiceRecordBtn.innerHTML = '🎙️ Speak Answer';
        }
        if (voiceRecordStatus) {
            voiceRecordStatus.textContent = "Voice recording stopped. You can edit your text before submitting.";
            voiceRecordStatus.style.color = "#94a3b8";
        }
    }

    // Toggle Webcam
    if (toggleWebcamBtn && webcamVideo) {
        toggleWebcamBtn.addEventListener('click', async () => {
            if (mediaStream) {
                // Turn off
                mediaStream.getTracks().forEach(track => track.stop());
                mediaStream = null;
                webcamVideo.srcObject = null;
                toggleWebcamBtn.textContent = "📷 Turn On Camera";
                webcamStatus.textContent = "Camera Off";
                webcamStatus.className = "badge badge-warning";
            } else {
                // Turn on
                try {
                    mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
                    webcamVideo.srcObject = mediaStream;
                    toggleWebcamBtn.textContent = "Turn Off Camera";
                    webcamStatus.textContent = "Live Video Active";
                    webcamStatus.className = "badge badge-success";
                } catch (err) {
                    alert("Unable to access webcam. Please check browser camera permissions.");
                }
            }
        });
    }

    // Web Speech API: Text-to-Speech (AI Voice)
    function speakText(text) {
        if (!('speechSynthesis' in window)) return;
        window.speechSynthesis.cancel(); // Stop any previous speech
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 1.0;
        utterance.pitch = 1.0;

        // Try to pick a natural English voice
        const voices = window.speechSynthesis.getVoices();
        const preferredVoice = voices.find(v => (v.name.includes("Google") || v.name.includes("Natural") || v.name.includes("Samantha") || v.name.includes("David")) && v.lang.startsWith("en"));
        if (preferredVoice) utterance.voice = preferredVoice;

        const aiAvatar = document.getElementById('aiInterviewerAvatar');
        if (aiAvatar) aiAvatar.classList.add('ai-speaking');

        utterance.onend = () => {
            if (aiAvatar) aiAvatar.classList.remove('ai-speaking');
        };

        window.speechSynthesis.speak(utterance);
    }

    if (speakQuestionBtn) {
        speakQuestionBtn.addEventListener('click', () => {
            const currentQ = questions[currentQuestionIndex];
            if (currentQ) speakText(currentQ.questionText);
        });
    }

    // Start Interview Button
    beginInterviewBtn.addEventListener('click', () => {
        beginInterviewBtn.disabled = true;
        beginInterviewBtn.innerHTML = '<span class="spinner"></span> Initializing AI Interviewer...';

        fetch('/api/interview/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ candidateId: parseInt(candidateId, 10) })
        })
        .then(async resp => {
            const data = await resp.json();
            if (!resp.ok) throw new Error(data.error || "Failed to start interview session.");
            return data;
        })
        .then(data => {
            sessionId = data.sessionId;
            questions = data.questions || [];
            currentQuestionIndex = 0;

            startScreen.style.display = 'none';
            activeScreen.style.display = 'block';

            startTimer();
            displayCurrentQuestion();
        })
        .catch(err => {
            alert(err.message);
            beginInterviewBtn.disabled = false;
            beginInterviewBtn.textContent = "Start Mock Interview";
        });
    });

    // Display Current Question
    function displayCurrentQuestion() {
        if (currentQuestionIndex >= questions.length) {
            finishInterview();
            return;
        }

        const q = questions[currentQuestionIndex];
        if (questionCategory) questionCategory.textContent = q.category || "Technical";
        if (questionCounter) questionCounter.textContent = `Question ${currentQuestionIndex + 1} of ${questions.length}`;
        if (questionText) questionText.textContent = q.questionText;

        if (answerInput) {
            answerInput.value = "";
            answerInput.focus();
        }

        // Auto speak question
        setTimeout(() => {
            speakText(q.questionText);
        }, 400);
    }

    // Submit Answer
    if (submitAnswerBtn) {
        submitAnswerBtn.addEventListener('click', () => {
            const currentQ = questions[currentQuestionIndex];
            if (!currentQ) return;

            stopVoiceRecording();
            const answer = answerInput.value.trim();
            if (answer.length < 10) {
                if (!confirm("Your answer is quite short. Are you sure you want to submit it now?")) {
                    return;
                }
            }

            submitAnswerBtn.disabled = true;
            submitAnswerBtn.innerHTML = 'Evaluating with AI...';

            fetch('/api/interview/submit-answer', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    questionId: currentQ.id,
                    answer: answer
                })
            })
            .then(async resp => {
                const data = await resp.json();
                if (!resp.ok) throw new Error(data.error || "Failed to evaluate answer.");
                return data;
            })
            .then(evalData => {
                showEvaluationModal(evalData);
            })
            .catch(err => {
                alert("Evaluation error: " + err.message);
                submitAnswerBtn.disabled = false;
                submitAnswerBtn.textContent = "Submit Answer & Evaluate";
            });
        });
    }

    // Skip Question
    if (skipQuestionBtn) {
        skipQuestionBtn.addEventListener('click', () => {
            if (confirm("Are you sure you want to skip this question? It will be marked with a baseline score.")) {
                const currentQ = questions[currentQuestionIndex];
                fetch('/api/interview/submit-answer', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        questionId: currentQ.id,
                        answer: "Candidate skipped this question."
                    })
                }).finally(() => {
                    currentQuestionIndex++;
                    displayCurrentQuestion();
                });
            }
        });
    }

    function showEvaluationModal(evalData) {
        if (evalScore) evalScore.textContent = `${evalData.score}/100`;
        if (evalStrengths) evalStrengths.textContent = evalData.strengths || "Addressed question concepts.";
        if (evalImprovements) evalImprovements.textContent = evalData.improvements || "Deepen your architectural rationale.";

        if (evalModal) evalModal.style.display = 'flex';
        submitAnswerBtn.disabled = false;
        submitAnswerBtn.textContent = "Submit Answer & Evaluate";
    }

    if (nextQuestionBtn) {
        nextQuestionBtn.addEventListener('click', () => {
            if (evalModal) evalModal.style.display = 'none';
            currentQuestionIndex++;
            displayCurrentQuestion();
        });
    }

    // Timer Countdown
    function startTimer() {
        updateTimerDisplay();
        timerInterval = setInterval(() => {
            remainingSeconds--;
            updateTimerDisplay();

            if (remainingSeconds <= 0) {
                clearInterval(timerInterval);
                alert("Time limit reached for the mock interview! Submitting your answers for final evaluation.");
                finishInterview();
            }
        }, 1000);
    }

    function updateTimerDisplay() {
        const mins = Math.floor(remainingSeconds / 60);
        const secs = remainingSeconds % 60;
        const formatted = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
        if (timerDisplay) timerDisplay.textContent = formatted;

        if (timerProgress) {
            const pct = Math.max(0, (remainingSeconds / totalSeconds) * 100);
            timerProgress.style.width = `${pct}%`;
            if (remainingSeconds < 120) {
                timerProgress.style.backgroundColor = '#ef4444';
            }
        }
    }

    // Finish Interview & Redirect to Scorecard
    function finishInterview() {
        if (timerInterval) clearInterval(timerInterval);
        stopVoiceRecording();
        if (mediaStream) {
            mediaStream.getTracks().forEach(track => track.stop());
        }

        if (activeScreen) {
            activeScreen.innerHTML = `
                <div class="card text-center" style="padding: 4rem 2rem;">
                    <div class="brand-icon" style="margin: 0 auto 1.5rem; width: 60px; height: 60px; font-size: 1.8rem;">🎓</div>
                    <h2>Synthesizing Final Dual Assessment...</h2>
                    <p class="text-sub" style="margin-top: 0.5rem;">Calculating ATS score, technical depth, articulation, and generating your personalized readiness roadmap.</p>
                    <div style="margin-top: 2rem;">
                        <span class="pulsing-dot" style="margin-right: 8px;"></span> AI Committee in Session...
                    </div>
                </div>
            `;
        }

        fetch('/api/interview/finish', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: sessionId })
        })
        .then(async resp => {
            const data = await resp.json();
            if (!resp.ok) throw new Error(data.error || "Failed to finalize interview report.");
            return data;
        })
        .then(data => {
            window.location.href = data.redirectUrl;
        })
        .catch(err => {
            alert("Error finalizing report: " + err.message);
            window.location.href = "/interview-report/" + candidateId;
        });
    }
}
