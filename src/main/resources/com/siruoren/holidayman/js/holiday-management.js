(function() {
    document.addEventListener('DOMContentLoaded', function() {
        var container = document.getElementById('holiday-data-container');
        if (!container) return;

        var rootUrl = container.getAttribute('data-rooturl');
        var tabs = container.querySelectorAll('.year-tab');
        var tableContainer = document.getElementById('holiday-table-container');

        if (tabs.length === 0) return;

        // Activate first tab
        activateTab(tabs[0]);

        tabs.forEach(function(tab) {
            tab.addEventListener('click', function(e) {
                e.preventDefault();
                activateTab(tab);
            });
        });

        function activateTab(tab) {
            tabs.forEach(function(t) {
                t.classList.remove('active');
            });
            tab.classList.add('active');
            var year = tab.getAttribute('data-year');
            loadHolidayData(year);
        }

        function loadHolidayData(year) {
            var url = rootUrl + 'manage/holiday-management/holidaysJson?year=' + year;
            fetch(url)
                .then(function(response) { return response.json(); })
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
                html += '<td><button class="btn-delete" onclick="deleteHoliday(\'' + escapeHtml(item.date) + '\')">Delete</button></td>';
                html += '</tr>';
            });
            html += '</tbody></table>';
            tableContainer.innerHTML = html;
        }

        function escapeHtml(str) {
            if (!str) return '';
            return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }
    });

    window.deleteHoliday = function(date) {
        if (!confirm('Delete holiday entry for ' + date + '?')) return;
        
        // Fetch crumb token for CSRF protection
        fetch(rootUrl + 'crumbIssuer/api/json')
            .then(function(response) { return response.json(); })
            .then(function(crumbData) {
                var form = document.createElement('form');
                form.method = 'POST';
                form.action = rootUrl + 'manage/holiday-management/deleteHoliday';
                
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
            .catch(function(err) {
                alert('Failed to get security token: ' + err.message);
            });
    };
})();
