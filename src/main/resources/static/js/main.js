// Основной JavaScript файл для логистической системы

// Глобальные переменные
let currentUser = null;
let currentRole = null;

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    initSidebar();
    initDatePickers();
    fetchCurrentUser();
});

// Инициализация бокового меню
function initSidebar() {
    document.querySelectorAll('.has-submenu').forEach(item => {
        const title = item.querySelector('.menu-title');
        title.addEventListener('click', (e) => {
            e.stopPropagation();
            item.classList.toggle('open');
            const submenu = item.querySelector('.submenu');
            if (submenu) submenu.classList.toggle('show');
        });
    });
}

// Инициализация datepicker с ограничением дат
function initDatePickers() {
    const today = new Date().toISOString().split('T')[0];
    const dateInputs = document.querySelectorAll('input[type="date"]');
    dateInputs.forEach(input => {
        if (input.id.includes('Start') || input.id.includes('End') || input.id === 'tripDate') {
            input.min = today;
        }
    });
}

// Получение текущего пользователя
async function fetchCurrentUser() {
    try {
        const response = await fetch('/api/auth/current-user');
        if (response.ok) {
            currentUser = await response.json();
            currentRole = currentUser.role;
            document.body.setAttribute('data-role', currentRole);
            updateUIByRole();
        }
    } catch(e) {
        console.error('Не удалось получить данные пользователя');
    }
}

// Обновление UI в зависимости от роли
function updateUIByRole() {
    const adminElements = document.querySelectorAll('.admin-only');
    const logistElements = document.querySelectorAll('.logist-only');
    const dispatcherElements = document.querySelectorAll('.dispatcher-only');

    if (currentRole === 'ADMIN') {
        adminElements.forEach(el => el.style.display = 'block');
        logistElements.forEach(el => el.style.display = 'block');
        dispatcherElements.forEach(el => el.style.display = 'block');
    } else if (currentRole === 'LOGIST') {
        adminElements.forEach(el => el.style.display = 'none');
        logistElements.forEach(el => el.style.display = 'block');
        dispatcherElements.forEach(el => el.style.display = 'none');
    } else if (currentRole === 'DISPATCHER') {
        adminElements.forEach(el => el.style.display = 'none');
        logistElements.forEach(el => el.style.display = 'none');
        dispatcherElements.forEach(el => el.style.display = 'block');
    }
}

// Форматирование даты
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU');
}

// Форматирование времени
function formatTime(timeString) {
    if (!timeString) return '-';
    return timeString.substring(0, 5);
}

// Получение цвета статуса
function getStatusColor(status) {
    const colors = {
        'NEW': '#fef3c7',
        'IN_PROGRESS': '#dbeafe',
        'PROCESSED': '#d1fae5',
        'COMPLETED': '#d1fae5'
    };
    return colors[status] || '#f3f4f6';
}

// Получение текста статуса
function getStatusText(status) {
    const texts = {
        'NEW': 'Новая',
        'IN_PROGRESS': 'В работе',
        'PROCESSED': 'Обработана',
        'COMPLETED': 'Завершена'
    };
    return texts[status] || status;
}

// Уведомления (заглушка)
function showNotification(message, type = 'info') {
    // В реальной системе здесь был бы Toast уведомление
    console.log(`[${type.toUpperCase()}] ${message}`);

    // Простое всплывающее уведомление
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        padding: 12px 20px;
        background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#3b82f6'};
        color: white;
        border-radius: 10px;
        z-index: 9999;
        animation: slideIn 0.3s ease;
    `;
    document.body.appendChild(notification);
    setTimeout(() => notification.remove(), 3000);
}

// Подтверждение действия
function confirmAction(message, callback) {
    if (confirm(message)) callback();
}

// Экспорт в Excel (заглушка)
function exportToExcel(data, filename) {
    console.log('Экспорт в Excel:', data);
    showNotification('Экспорт в Excel (заглушка)', 'info');
}

// Печать страницы
function printPage() {
    window.print();
}

// Обновление статуса из системы диспетчеризации (заглушка)
async function syncWithDispatchSystem(tripId) {
    try {
        const response = await fetch(`/api/dispatch/sync/${tripId}`, { method: 'POST' });
        if (response.ok) {
            showNotification('Статус обновлен из системы диспетчеризации', 'success');
            return true;
        }
    } catch(e) {
        console.error('Ошибка синхронизации');
    }
    return false;
}

// Проверка синхронизации с 1С (заглушка)
async function check1CSync(requestId) {
    try {
        const response = await fetch(`/api/1c/sync-status/${requestId}`);
        const data = await response.json();
        if (data.status === 'COMPLETED') {
            showNotification('Данные синхронизированы с 1С', 'success');
        }
        return data;
    } catch(e) {
        console.error('Ошибка проверки синхронизации с 1С');
    }
}

// Автоматическое обновление данных (polling)
let pollingInterval = null;

function startAutoRefresh(intervalSeconds = 30) {
    if (pollingInterval) clearInterval(pollingInterval);
    pollingInterval = setInterval(() => {
        if (window.location.pathname.includes('/request-detail')) {
            const requestId = new URLSearchParams(window.location.search).get('id');
            if (requestId && typeof loadRequest === 'function') loadRequest();
        } else if (window.location.pathname.includes('/requests')) {
            if (typeof loadRequests === 'function') loadRequests();
        }
    }, intervalSeconds * 1000);
}

function stopAutoRefresh() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

// Запуск автообновления на страницах с динамическими данными
if (window.location.pathname.includes('/request-detail') ||
    window.location.pathname.includes('/requests') ||
    window.location.pathname.includes('/trips')) {
    startAutoRefresh(30);
}

// CSS анимация для уведомлений
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
`;
document.head.appendChild(style);

// Экспорт функций в глобальный контекст
window.showNotification = showNotification;
window.confirmAction = confirmAction;
window.exportToExcel = exportToExcel;
window.printPage = printPage;
window.formatDate = formatDate;
window.formatTime = formatTime;
window.getStatusText = getStatusText;
window.getStatusColor = getStatusColor;
window.syncWithDispatchSystem = syncWithDispatchSystem;
window.check1CSync = check1CSync;
window.startAutoRefresh = startAutoRefresh;
window.stopAutoRefresh = stopAutoRefresh;