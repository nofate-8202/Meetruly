document.addEventListener('DOMContentLoaded', function() {
    console.log('Document loaded, initializing like buttons');

    
    const likeButtons = document.querySelectorAll('.like-btn');
    console.log(`Found ${likeButtons.length} like buttons`);

    likeButtons.forEach(button => {
        button.addEventListener('click', function() {
            const userId = this.getAttribute('data-user-id');
            const isLiked = this.classList.contains('liked');
            const method = isLiked ? 'DELETE' : 'POST';

            console.log(`Like button clicked for user ${userId}, current state: ${isLiked ? 'liked' : 'not liked'}`);
            console.log(`Sending ${method} request to /matching/like/${userId}`);

            
            this.disabled = true;

            
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

            console.log(`CSRF Token: ${csrfToken ? 'Found' : 'Not found'}`);

            
            const headers = {
                'Content-Type': 'application/json'
            };

            
            if (csrfToken) {
                headers[csrfHeader] = csrfToken;
            }

            
            fetch(`/matching/like/${userId}`, {
                method: method,
                headers: headers,
                credentials: 'same-origin' 
            })
                .then(response => {
                    console.log(`Response status: ${response.status}`);
                    if (response.ok) {
                        
                        if (isLiked) {
                            
                            this.innerHTML = '<i class="bi bi-heart"></i> Like';
                            this.classList.remove('liked', 'btn-primary');
                            this.classList.add('btn-outline-primary');
                            showNotification('Success', 'You have unliked this profile', 'success');
                            console.log('Profile unliked successfully');
                        } else {
                            
                            this.innerHTML = '<i class="bi bi-heart-fill"></i> Liked';
                            this.classList.add('liked', 'btn-primary');
                            this.classList.remove('btn-outline-primary');
                            showNotification('Success', 'You liked this profile!', 'success');
                            console.log('Profile liked successfully');

                            
                            return response.json().catch(() => {
                                console.log('No JSON response or empty response');
                                return null;
                            }).then(data => {
                                if (data && data.matchCreated) {
                                    showNotification('Match!', 'You and this user have matched. You can now chat!', 'success');
                                    console.log('Match created!');
                                }
                            });
                        }
                    } else {
                        return response.text().then(text => {
                            console.error(`Error response: ${text}`);
                            throw new Error(text || 'Unknown error occurred');
                        });
                    }
                })
                .catch(error => {
                    console.error('Error with like/unlike:', error);
                    
                    if (error.message.includes('daily message limit')) {
                        showNotification('Error', 'You have reached your daily limit. Please upgrade your subscription plan.', 'error');
                    } else {
                        showNotification('Error', 'Failed to process like action. Please try again.', 'error');
                    }
                })
                .finally(() => {
                    
                    this.disabled = false;
                    console.log('Button re-enabled');
                });
        });
    });

    
});