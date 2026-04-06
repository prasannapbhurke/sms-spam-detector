document.addEventListener('DOMContentLoaded', () => {
    const checkButton = document.getElementById('checkButton');
    const messageInput = document.getElementById('messageInput');
    const resultContainer = document.getElementById('resultContainer');
    const predictionResult = document.getElementById('predictionResult');
    const spamScoreBar = document.getElementById('spamScoreBar');
    const spamScoreText = document.getElementById('spamScoreText');
    const insightText = document.getElementById('insightText');

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

        predictionResult.textContent = isSpam ? `🚨 SPAM DETECTED (${confidence}%)` : `✅ SAFE MESSAGE (${confidence}%)`;
        resultContainer.classList.add(isSpam ? 'spam' : 'safe');

        // Logic: if Spam, show confidence as spam prob. If Not Spam, show (100 - confidence) for spam bar.
        let spamBarValue = isSpam ? parseFloat(confidence) : (100 - parseFloat(confidence));

        spamScoreBar.style.width = spamBarValue + '%';
        spamScoreText.textContent = spamBarValue.toFixed(1) + '%';

        if (isSpam) {
            insightText.textContent = "This message matches patterns commonly found in spam or phishing attempts.";
        } else {
            insightText.textContent = "This message appears to be safe based on our analysis.";
        }
    }
});
