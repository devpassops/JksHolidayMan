(function() {
    var rootUrl = '';

    function getRootUrl() {
        if (rootUrl) return rootUrl;
        var root = document.getElementById('holiday-mgmt-root');
        if (root) {
            rootUrl = root.getAttribute('data-rooturl') || '';
        }
        if (!rootUrl) {
            var container = document.getElementById('holiday-data-container');
            if (container) {
                rootUrl = container.getAttribute('data-rooturl') || '';
            }
        }
        return rootUrl;
    }

    function getCrumb() {
        var url = getRootUrl() + 'crumbIssuer/api/json';
        return fetch(url, { credentials: 'same-origin' })
            .then(function(response) {
                if (!response.ok) throw new Error('Failed to fetch crumb: ' + response.status);
                return response.json();
            });
    }

    function postWithCrumb(url, params) {
        return getCrumb().then(function(crumbData) {
            var formData = new FormData();
            formData.append(crumbData.crumbRequestField, crumbData.crumb);
            for (var key in params) {
                if (params.hasOwnProperty(key)) {
                    formData.append(key, params[key]);
                }
            }
            return fetch(url, {
                method: 'POST',
                credentials: 'same-origin',
                body: formData
            });
        }).then(function(response) {
            if (response.ok) {
                window.location.reload();
            } else {
                response.text().then(function(text) {
                    alert('Operation failed: ' + response.status + ' - ' + text.substring(0, 200));
                });
            }
        }).catch(function(err) {
            alert('Error: ' + err.message);
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        var container = document.getElementById('holiday-data-container');
        if (!container) return;

        var tabs = container.querySelectorAll('.year-tab');
        var tableContainer = document.getElementById('holiday-table-container');

        if (tabs.length === 0) return;

        // Find and activate current year tab, or first tab
        var currentYear = new Date().getFullYear();
        var currentYearTab = null;
        tabs.forEach(function(tab) {
            if (parseInt(tab.getAttribute('data-year')) === currentYear) {
                currentYearTab = tab;
            }
        });
        activateTab(currentYearTab || tabs[0]);

        // Add click handlers for year tabs
        tabs.forEach(function(tab) {
            tab.addEventListener('click', function(e) {
                e.preventDefault();
                activateTab(tab);
            });
        });

        // Add click handlers for delete year buttons
        var deleteYearBtns = container.querySelectorAll('.delete-year-btn');
        deleteYearBtns.forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                e.preventDefault();
                var year = btn.getAttribute('data-year');
                if (confirm('Delete all holiday data for year ' + year + '? This action cannot be undone.')) {
                    var url = getRootUrl() + 'manage/holiday-management/deleteYear';
                    postWithCrumb(url, { year: year });
                }
            });
        });

        function activateTab(tab) {
            tabs.forEach(function(t) {
                t.classList.remove('active');
                t.style.backgroundColor = '#e8e8e8';
                t.style.color = '#333';
            });
            tab.classList.add('active');
            tab.style.backgroundColor = '#4a90d9';
            tab.style.color = '#fff';
            var year = tab.getAttribute('data-year');
            loadHolidayData(year);
        }

        function loadHolidayData(year) {
            var url = getRootUrl() + 'manage/holiday-management/holidaysJson?year=' + year;
            fetch(url, { credentials: 'same-origin' })
                .then(function(response) {
                    if (!response.ok) throw new Error('HTTP ' + response.status);
                    return response.json();
                })
                .then(function(data) { renderTable(data, year); })
                .catch(function(err) {
                    tableContainer.innerHTML = '<p style="color:red;">Failed to load data: ' + err.message + '</p>';
                });
        }

        function renderTable(data, year) {
            if (!data || data.length === 0) {
                tableContainer.innerHTML = '<p style="color:#666;">No holiday data for ' + year + '</p>';
                return;
            }

            var html = '<table>';
            html += '<thead><tr><th>Date</th><th>Name</th><th>Type</th><th>Action</th></tr></thead>';
            html += '<tbody>';
            data.forEach(function(item) {
                var typeClass = item.isHoliday ? 'badge-holiday' : 'badge-workday';
                var typeLabel = item.isHoliday ? 'Holiday' : 'Workday';
                html += '<tr>';
                html += '<td>' + escapeHtml(item.date) + '</td>';
                html += '<td>' + escapeHtml(item.name) + '</td>';
                html += '<td><span class="' + typeClass + '">' + typeLabel + '</span></td>';
                html += '<td><button class="btn-delete" data-date="' + escapeHtml(item.date) + '">Delete</button></td>';
                html += '</tr>';
            });
            html += '</tbody></table>';
            tableContainer.innerHTML = html;

            // Add click handlers for delete buttons
            tableContainer.querySelectorAll('.btn-delete').forEach(function(btn) {
                btn.addEventListener('click', function() {
                    var date = btn.getAttribute('data-date');
                    if (confirm('Delete holiday entry for ' + date + '?')) {
                        var url = getRootUrl() + 'manage/holiday-management/deleteHoliday';
                        postWithCrumb(url, { date: date });
                    }
                });
            });
        }

        function escapeHtml(str) {
            if (!str) return '';
            return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }
    });
})();
