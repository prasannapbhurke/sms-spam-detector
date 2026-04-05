document.addEventListener('DOMContentLoaded', function() {
    const messageTextarea = document.getElementById('message');
    const analyzeBtn = document.getElementById('analyze-btn');
    const resultDiv = document.getElementById('result');
    const insightDiv = document.getElementById('insight'); // This will now be the container for detailed insights
    const spamBar = document.getElementById('spam-bar');
    const safeBar = document.getElementById('safe-bar');
    const historyDiv = document.getElementById('history');
    const clearBtn = document.getElementById('clear-btn');

    analyzeBtn.addEventListener('click', analyzeMessage);
    clearBtn.addEventListener('click', clearHistory);
    
    // Allow analysis by pressing Ctrl+Enter or Cmd+Enter
    messageTextarea.addEventListener('keydown', function(e) {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            analyzeMessage();
        }
    });

    function analyzeMessage() {
        const message = messageTextarea.value.trim();
        if (!message) {
            alert('Please enter a message to analyze.');
            return;
        }

        // Show loading state
        analyzeBtn.textContent = 'Analyzing...';
        analyzeBtn.disabled = true;
        resultDiv.innerHTML = '';
        insightDiv.innerHTML = '';
        spamBar.style.width = '0%';
        safeBar.style.width = '0%';

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

            // Main result text
            resultDiv.textContent = data.result;
            resultDiv.style.color = data.color;

            // Display detailed insights
            insightDiv.innerHTML = '<h3>Detailed Analysis:</h3>';
            const insightList = document.createElement('ul');
            insightList.className = 'insight-list';
            data.detailed_insight.forEach(item => {
                const listItem = document.createElement('li');
                listItem.innerHTML = `
                    <span class="insight-emoji">${item.emoji}</span>
                    <div class="insight-text">
                        <strong>${item.reason}</strong>
                        <p>${item.description}</p>
                    </div>
                `;
                insightList.appendChild(listItem);
            });
            insightDiv.appendChild(insightList);

            // Update progress bars
            spamBar.style.width = `${data.spam_score}%`;
            safeBar.style.width = `${data.safe_score}%`;

            // Add to history
            const historyEntry = document.createElement('div');
            historyEntry.className = 'history-entry';
            historyEntry.innerHTML = `<p class="history-message">"${message}"</p><p class="history-result" style="color:${data.color};">${data.result}</p>`;
            historyDiv.prepend(historyEntry); // Prepend to show newest first
        })
        .catch(error => {
            console.error('Error:', error);
            resultDiv.textContent = 'An error occurred while analyzing the message.';
            resultDiv.style.color = 'red';
        })
        .finally(() => {
            // Reset button state
            analyzeBtn.textContent = '🔍 Analyze Message';
            analyzeBtn.disabled = false;
        });
    }

    function clearHistory() {
        historyDiv.innerHTML = '';
    }
});
