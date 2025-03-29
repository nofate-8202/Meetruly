


document.addEventListener('DOMContentLoaded', function() {

    
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function(tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function(popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });

    
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const navbarNav = document.getElementById('navbarNav');

    if (mobileMenuToggle && navbarNav) {
        mobileMenuToggle.addEventListener('click', function() {
            navbarNav.classList.toggle('show');
        });
    }

    
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();

            const targetId = this.getAttribute('href');
            if (targetId === '#') return;

            const targetElement = document.querySelector(targetId);

            if (targetElement) {
                window.scrollTo({
                    top: targetElement.offsetTop - 100,
                    behavior: 'smooth'
                });
            }
        });
    });

    
    const backToTopButton = document.getElementById('backToTop');

    if (backToTopButton) {
        
        window.addEventListener('scroll', function() {
            if (window.pageYOffset > 300) {
                backToTopButton.classList.add('show');
            } else {
                backToTopButton.classList.remove('show');
            }
        });

        
        backToTopButton.addEventListener('click', function() {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }

    
    const forms = document.querySelectorAll('.needs-validation');

    Array.from(forms).forEach(form => {
        form.addEventListener('submit', event => {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }

            form.classList.add('was-validated');
        }, false);
    });

    
    window.showNotification = function(message, type = 'success', duration = 3000) {
        
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;

        
        const notificationHeader = document.createElement('div');
        notificationHeader.className = 'notification-header';

        const notificationTitle = document.createElement('div');
        notificationTitle.className = 'notification-title';
        notificationTitle.textContent = type.charAt(0).toUpperCase() + type.slice(1);

        const notificationClose = document.createElement('div');
        notificationClose.className = 'notification-close';
        notificationClose.innerHTML = '&times;';
        notificationClose.addEventListener('click', () => {
            notification.remove();
        });

        notificationHeader.appendChild(notificationTitle);
        notificationHeader.appendChild(notificationClose);

        
        const notificationBody = document.createElement('div');
        notificationBody.className = 'notification-body';
        notificationBody.textContent = message;

        
        notification.appendChild(notificationHeader);
        notification.appendChild(notificationBody);

        
        document.body.appendChild(notification);

        
        setTimeout(() => {
            notification.classList.add('show');
        }, 10);

        
        setTimeout(() => {
            notification.classList.remove('show');
            setTimeout(() => {
                notification.remove();
            }, 300);
        }, duration);

        return notification;
    };

    
    window.ajaxForm = function(formElement, successCallback, errorCallback) {
        formElement.addEventListener('submit', function(e) {
            e.preventDefault();

            const formData = new FormData(this);
            const url = this.getAttribute('action');
            const method = this.getAttribute('method') || 'POST';

            fetch(url, {
                method: method,
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    return response.json();
                })
                .then(data => {
                    if (typeof successCallback === 'function') {
                        successCallback(data);
                    }
                })
                .catch(error => {
                    if (typeof errorCallback === 'function') {
                        errorCallback(error);
                    } else {
                        console.error('Error:', error);
                    }
                });
        });
    };

    
    const dropdowns = document.querySelectorAll('.dropdown');

    if (window.innerWidth >= 992) { 
        dropdowns.forEach(dropdown => {
            dropdown.addEventListener('mouseenter', function() {
                const dropdownMenu = this.querySelector('.dropdown-menu');
                if (dropdownMenu) {
                    dropdownMenu.classList.add('show');
                }
            });

            dropdown.addEventListener('mouseleave', function() {
                const dropdownMenu = this.querySelector('.dropdown-menu');
                if (dropdownMenu) {
                    dropdownMenu.classList.remove('show');
                }
            });
        });
    }
});