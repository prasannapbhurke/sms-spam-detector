document.addEventListener('DOMContentLoaded', () => {
    const checkButton = document.getElementById('checkButton');
    const messageInput = document.getElementById('messageInput');
    const resultContainer = document.getElementById('resultContainer');
    const predictionResult = document.getElementById('predictionResult');
    const spamScoreBar = document.getElementById('spamScoreBar');
    const spamScoreText = document.getElementById('spamScoreText');
    const insightText = document.getElementById('insightText');

    // UNIFIED API KEY - MUST MATCH app.py EXACTLY
    const API_KEY = "SMS_GUARD_2024_SECURE";

    checkButton.addEventListener('click', async () => {
        const message = messageInput.value.trim();

        if (!message) {
            alert('Please enter a message.');
            return;
        }

        // Reset UI
        resultContainer.classList.add('hidden');
        checkButton.disabled = true;
        checkButton.textContent = 'Analyzing...';

        try {
            const response = await fetch('/predict', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-API-KEY': API_KEY
                },
                body: JSON.stringify({ message: message }),
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'Analysis failed. Please try again.');
            }

            displayResult(data);

        } catch (error) {
            console.error('Error:', error);
            alert('Error analyzing message: ' + error.message);
        } finally {
            checkButton.disabled = false;
            checkButton.textContent = 'Analyze Message';
        }
    });

    function displayResult(data) {
        resultContainer.classList.remove('hidden');
        resultContainer.classList.remove('spam', 'safe');

        const isSpam = data.prediction === 'Spam';
        const confidence = data.confidence || 0;

        predictionResult.textContent = isSpam ? `🚨 SPAM DETECTED (${(confidence * 100).toFixed(1)}%)` : `✅ SAFE MESSAGE (${(confidence * 100).toFixed(1)}%)`;
        resultContainer.classList.add(isSpam ? 'spam' : 'safe');

        let spamBarValue = isSpam ? parseFloat(confidence) * 100 : (1 - parseFloat(confidence)) * 100;

        spamScoreBar.style.width = spamBarValue + '%';
        spamScoreText.textContent = spamBarValue.toFixed(1) + '%';

        if (isSpam) {
            insightText.textContent = "This message matches patterns commonly found in spam or phishing attempts.";
        } else {
            insightText.textContent = "This message appears to be safe based on our analysis.";
        }
    }
});
