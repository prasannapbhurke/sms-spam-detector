document.addEventListener('DOMContentLoaded', function() {
    const messageTextarea = document.getElementById('message');
    const analyzeBtn = document.getElementById('analyze-btn');
    const resultDiv = document.getElementById('result');
    const insightDiv = document.getElementById('insight');
    const spamBar = document.getElementById('spam-bar');
    const safeBar = document.getElementById('safe-bar');
    const historyDiv = document.getElementById('history');
    const clearBtn = document.getElementById('clear-btn');

    analyzeBtn.addEventListener('click', analyzeMessage);
    clearBtn.addEventListener('click', clearHistory);

    function analyzeMessage() {
        const message = messageTextarea.value.trim();
        if (!message) {
            alert('Please enter a message to analyze.');
            return;
        }

        fetch('/predict', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ message: message }),
        })
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                resultDiv.textContent = `Error: ${data.error}`;
                resultDiv.style.color = 'orange';
                return;
            }

            resultDiv.textContent = data.result;
            resultDiv.style.color = data.color;
            insightDiv.textContent = data.insight;

            spamBar.style.width = `${data.spam_score}%`;
            safeBar.style.width = `${data.safe_score}%`;

            // Add to history
            const historyEntry = document.createElement('div');
            historyEntry.innerHTML = `<strong>${message}</strong><br>→ ${data.result}<br><br>`;
            historyDiv.appendChild(historyEntry);
            historyDiv.scrollTop = historyDiv.scrollHeight;
        })
        .catch(error => {
            console.error('Error:', error);
            resultDiv.textContent = 'An error occurred while analyzing the message.';
            resultDiv.style.color = 'red';
        });
    }

    function clearHistory() {
        historyDiv.innerHTML = '';
    }
});
