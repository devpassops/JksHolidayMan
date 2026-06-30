(function () {
    "use strict";

    function getRootUrl() {
        var el = document.getElementById("holiday-data-container");
        if (el && el.dataset.rooturl) return el.dataset.rooturl;
        return document.head.dataset.rooturl || "";
    }

    function loadYearHolidays(year) {
        var container = document.getElementById("holiday-table-container");
        if (!container) return;

        var url = getRootUrl() + "manage/holiday-management/holidaysJson?year=" + year;

        fetch(url)
            .then(function (response) { return response.json(); })
            .then(function (data) {
                renderHolidayTable(container, data, year);
            })
            .catch(function (err) {
                container.innerHTML = '<p style="color:red;">Failed to load holiday data: ' + err.message + '</p>';
            });
    }

    function renderHolidayTable(container, holidays, year) {
        if (!holidays || holidays.length === 0) {
            container.innerHTML = '<p style="color:#666;">No holiday data for year ' + year + '</p>';
            return;
        }

        var html = '<table class="holiday-table">';
        html += '<thead><tr><th>Date</th><th>Name</th><th>Type</th><th>Action</th></tr></thead>';
        html += '<tbody>';

        holidays.forEach(function (h) {
            var typeClass = h.isHoliday ? 'type-holiday' : 'type-workday';
            var typeLabel = h.isHoliday ? 'Holiday' : 'Workday (Adjusted)';
            html += '<tr>';
            html += '<td>' + h.date + '</td>';
            html += '<td>' + (h.name || '-') + '</td>';
            html += '<td class="' + typeClass + '">' + typeLabel + '</td>';
            html += '<td><a href="#" class="delete-btn" data-date="' + h.date + '" title="Delete">Delete</a></td>';
            html += '</tr>';
        });

        html += '</tbody></table>';

        var holidayCount = holidays.filter(function (h) { return h.isHoliday; }).length;
        var workdayCount = holidays.filter(function (h) { return !h.isHoliday; }).length;
        html += '<p style="margin-top:8px;color:#666;font-size:12px;">';
        html += 'Total: ' + holidays.length + ' entries';
        html += ' (Holidays: ' + holidayCount + ', Adjusted Workdays: ' + workdayCount + ')';
        html += '</p>';

        container.innerHTML = html;

        // Attach delete handlers
        container.querySelectorAll('.delete-btn').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                if (confirm('Are you sure you want to delete the holiday entry for ' + btn.dataset.date + '?')) {
                    deleteHoliday(btn.dataset.date, year);
                }
            });
        });
    }

    function deleteHoliday(date, year) {
        // Fetch crumb token for CSRF protection
        fetch(getRootUrl() + 'crumbIssuer/api/json')
            .then(function (response) { return response.json(); })
            .then(function (crumbData) {
                var form = document.createElement('form');
                form.method = 'POST';
                form.action = getRootUrl() + 'manage/holiday-management/deleteHoliday';
                
                // Add crumb token
                var crumbInput = document.createElement('input');
                crumbInput.type = 'hidden';
                crumbInput.name = crumbData.crumbRequestField;
                crumbInput.value = crumbData.crumb;
                form.appendChild(crumbInput);
                
                // Add date parameter
                var dateInput = document.createElement('input');
                dateInput.type = 'hidden';
                dateInput.name = 'date';
                dateInput.value = date;
                form.appendChild(dateInput);
                
                document.body.appendChild(form);
                form.submit();
            })
            .catch(function (err) {
                alert('Failed to get security token: ' + err.message);
            });
    }

    function initYearTabs() {
        var tabs = document.querySelectorAll('.year-tab');
        if (tabs.length === 0) return;

        // Find and activate current year tab, or first tab if current year not found
        var currentYear = new Date().getFullYear();
        var currentYearTab = null;
        tabs.forEach(function (tab) {
            if (parseInt(tab.dataset.year) === currentYear) {
                currentYearTab = tab;
            }
        });

        // Activate the current year tab or first tab
        var activeTab = currentYearTab || tabs[0];
        activeTab.classList.add('active');
        activeTab.style.backgroundColor = '#4a90d9';
        activeTab.style.color = '#fff';
        loadYearHolidays(activeTab.dataset.year);

        // Add click handlers for year tabs
        tabs.forEach(function (tab) {
            tab.addEventListener('click', function (e) {
                e.preventDefault();
                tabs.forEach(function (t) { 
                    t.classList.remove('active'); 
                    t.style.backgroundColor = '#e8e8e8';
                    t.style.color = '#333';
                });
                tab.classList.add('active');
                tab.style.backgroundColor = '#4a90d9';
                tab.style.color = '#fff';
                loadYearHolidays(tab.dataset.year);
            });
        });

        // Add click handlers for delete year buttons
        var deleteYearBtns = document.querySelectorAll('.delete-year-btn');
        deleteYearBtns.forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                var year = btn.dataset.year;
                deleteYear(year);
            });
        });
    }

    function deleteYear(year) {
        if (!confirm('Delete all holiday data for year ' + year + '? This action cannot be undone.')) return;
        
        // Fetch crumb token for CSRF protection
        fetch(getRootUrl() + 'crumbIssuer/api/json')
            .then(function (response) { return response.json(); })
            .then(function (crumbData) {
                var form = document.createElement('form');
                form.method = 'POST';
                form.action = getRootUrl() + 'manage/holiday-management/deleteYear';
                
                // Add crumb token
                var crumbInput = document.createElement('input');
                crumbInput.type = 'hidden';
                crumbInput.name = crumbData.crumbRequestField;
                crumbInput.value = crumbData.crumb;
                form.appendChild(crumbInput);
                
                // Add year parameter
                var yearInput = document.createElement('input');
                yearInput.type = 'hidden';
                yearInput.name = 'year';
                yearInput.value = year;
                form.appendChild(yearInput);
                
                document.body.appendChild(form);
                form.submit();
            })
            .catch(function (err) {
                alert('Failed to get security token: ' + err.message);
            });
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initYearTabs);
    } else {
        initYearTabs();
    }
})();
