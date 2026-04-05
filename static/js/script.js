document.addEventListener('DOMContentLoaded', function() {
    const messageTextarea = document.getElementById('message');
    const resultDiv = document.getElementById('result');
    const insightDiv = document.getElementById('insight');
    const spamBar = document.getElementById('spam-bar');
    const safeBar = document.getElementById('safe-bar');
    const historyDiv = document.getElementById('history');
    const clearBtn = document.getElementById('clear-btn');
    const analysisStatus = document.getElementById('analysis-status');

    // --- Real-time Analysis Logic ---
    let debounceTimeout;
    messageTextarea.addEventListener('input', () => {
        // Clear previous timeout
        clearTimeout(debounceTimeout);

        const message = messageTextarea.value.trim();
        
        if (!message) {
            // Instantly clear results if textarea is empty
            clearResults();
            analysisStatus.textContent = 'Start typing to analyze...';
            return;
        }

        analysisStatus.textContent = 'Typing...';

        // Set a new timeout
        debounceTimeout = setTimeout(() => {
            analyzeMessage(message);
        }, 500); // 500ms delay after user stops typing
    });

    clearBtn.addEventListener('click', clearHistory);

    function clearResults() {
        resultDiv.innerHTML = '';
        insightDiv.innerHTML = '';
        spamBar.style.width = '0%';
        safeBar.style.width = '0%';
    }

    function analyzeMessage(message) {
        analysisStatus.textContent = 'Analyzing...';

        fetch('/predict', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: message }),
        })
        .then(response => response.json())
        .then(data => {
            analysisStatus.textContent = 'Analysis Complete';
            
            if (data.error) {
                resultDiv.textContent = `Error: ${data.error}`;
                resultDiv.style.color = 'orange';
                return;
            }

            resultDiv.textContent = data.result;
            resultDiv.style.color = data.color;

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

            spamBar.style.width = `${data.spam_score}%`;
            safeBar.style.width = `${data.safe_score}%`;

            const historyEntry = document.createElement('div');
            historyEntry.className = 'history-entry';
            historyEntry.innerHTML = `<p class="history-message">"${message}"</p><p class="history-result" style="color:${data.color};">${data.result}</p>`;
            historyDiv.prepend(historyEntry);
        })
        .catch(error => {
            console.error('Error:', error);
            analysisStatus.textContent = 'Analysis Failed';
            resultDiv.textContent = 'An error occurred during analysis.';
            resultDiv.style.color = 'red';
        });
    }

    function clearHistory() {
        historyDiv.innerHTML = '';
    }
});
