(function() {
    /*
     * URL compatibility strategy for both deployment modes:
     * - Java direct start: Jenkins at http://host:port/ (no context path)
     * - Tomcat deployment: Jenkins at http://host:port/jenkins/ (with context path)
     *
     * Page-level operations (holidaysJson, deleteHoliday, deleteYear, importYear, importFile):
     *   Use relative URLs, browser resolves them based on current page URL.
     *   Works with any context path automatically.
     *
     * Jenkins root-level operations (crumbIssuer):
     *   Use Jenkins-provided rootURL from data attribute, which always includes
     *   the correct context path.
     */

    function getJenkinsRootUrl() {
        var el = document.getElementById("holiday-data-container");
        if (el && el.dataset.jenkinsroot) return el.dataset.jenkinsroot;
        // Fallback: compute from current page URL for /manage/{urlName}/ pattern
        var loc = window.location;
        var path = loc.pathname;
        if (path.endsWith('/')) path = path.slice(0, -1);
        var segments = path.split('/');
        // Remove last 2 segments (manage, holiday-management) to get context path
        segments.splice(segments.length - 2, 2);
        var contextPath = segments.join('/') || '';
        return loc.origin + contextPath + '/';
    }

    function getCrumb() {
        var crumbUrl = getJenkinsRootUrl() + 'crumbIssuer/api/json';
        return fetch(crumbUrl, { credentials: 'same-origin' })
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Failed to fetch crumb: ' + response.status);
                }
                var contentType = response.headers.get('content-type');
                if (!contentType || !contentType.includes('application/json')) {
                    throw new Error('Not authenticated or crumb issuer disabled');
                }
                return response.json();
            });
    }

    /**
     * POST with crumb using URLSearchParams (application/x-www-form-urlencoded).
     * The crumb is sent BOTH as a form parameter AND as a request header.
     */
    function postWithCrumb(url, params) {
        return getCrumb().then(function(crumbData) {
            var body = new URLSearchParams();
            body.append(crumbData.crumbRequestField, crumbData.crumb);
            for (var key in params) {
                if (params.hasOwnProperty(key)) {
                    body.append(key, params[key]);
                }
            }
            var headers = {};
            headers[crumbData.crumbRequestField] = crumbData.crumb;
            headers['Content-Type'] = 'application/x-www-form-urlencoded';
            headers['X-Requested-With'] = 'XMLHttpRequest';
            return fetch(url, {
                method: 'POST',
                credentials: 'same-origin',
                headers: headers,
                body: body.toString()
            });
        }).then(function(response) {
            return response.json();
        }).then(function(data) {
            if (data.success) {
                alert(data.message);
                window.location.reload();
            } else {
                alert(data.message || 'Operation failed');
            }
        }).catch(function(err) {
            alert('Error: ' + err.message);
        });
    }

    /**
     * File upload with crumb using FormData (multipart/form-data).
     * The crumb is sent as a request header (not in FormData body) because
     * Jenkins CrumbFilter cannot reliably parse crumb from multipart requests.
     */
    function uploadFileWithCrumb(url, formData) {
        return getCrumb().then(function(crumbData) {
            var headers = {};
            headers[crumbData.crumbRequestField] = crumbData.crumb;
            headers['X-Requested-With'] = 'XMLHttpRequest';
            return fetch(url, {
                method: 'POST',
                credentials: 'same-origin',
                headers: headers,
                body: formData
            });
        }).then(function(response) {
            return response.json();
        }).then(function(data) {
            if (data.success) {
                alert(data.message);
                window.location.reload();
            } else {
                alert(data.message || 'Import failed');
            }
        }).catch(function(err) {
            alert('Error: ' + err.message);
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        var container = document.getElementById('holiday-data-container');
        if (!container) return;

        // ===== Import Year (API) Handler =====
        var importYearBtn = document.getElementById('importYearBtn');
        if (importYearBtn) {
            importYearBtn.addEventListener('click', function() {
                var yearInput = document.getElementById('importYearInput');
                var year = yearInput ? parseInt(yearInput.value) : new Date().getFullYear();
                if (!year || year < 2020 || year > 2099) {
                    alert('Please enter a valid year (2020-2099)');
                    return;
                }
                if (confirm('Import holiday data for year ' + year + ' from API?')) {
                    postWithCrumb('importYear', { year: year });
                }
            });
        }

        // ===== Import File Handler =====
        var importFileBtn = document.getElementById('importFileBtn');
        if (importFileBtn) {
            importFileBtn.addEventListener('click', function() {
                var fileInput = document.getElementById('importFileInput');
                if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
                    alert('Please select a file before importing.');
                    return;
                }
                var file = fileInput.files[0];
                if (file.size > 2 * 1024 * 1024) {
                    alert('File too large. Maximum size is 2MB.');
                    return;
                }
                if (confirm('Import holiday data from file "' + file.name + '"?')) {
                    var formData = new FormData();
                    formData.append('file', file);
                    uploadFileWithCrumb('importFile', formData);
                }
            });
        }

        // ===== Holiday Data Display =====
        var tabs = container.querySelectorAll('.year-tab');
        var tableContainer = document.getElementById('holiday-table-container');

        if (tabs.length === 0) return;

        // Activate current year tab, or first tab
        var currentYear = new Date().getFullYear();
        var currentYearTab = null;
        tabs.forEach(function(tab) {
            if (parseInt(tab.getAttribute('data-year')) === currentYear) {
                currentYearTab = tab;
            }
        });
        activateTab(currentYearTab || tabs[0]);

        // Year tab click handlers
        tabs.forEach(function(tab) {
            tab.addEventListener('click', function(e) {
                e.preventDefault();
                activateTab(tab);
            });
        });

        // Delete year button handlers
        container.querySelectorAll('.delete-year-btn').forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                e.preventDefault();
                var year = btn.getAttribute('data-year');
                if (confirm('Delete all holiday data for year ' + year + '?')) {
                    postWithCrumb('deleteYear', { year: year });
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
            loadHolidayData(tab.getAttribute('data-year'));
        }

        function loadHolidayData(year) {
            fetch('holidaysJson?year=' + year, { credentials: 'same-origin' })
                .then(function(response) {
                    if (!response.ok) throw new Error('HTTP ' + response.status);
                    var contentType = response.headers.get('content-type');
                    if (!contentType || !contentType.includes('application/json')) {
                        throw new Error('Not authenticated or permission denied');
                    }
                    return response.json();
                })
                .then(function(data) { renderTable(data, year); })
                .catch(function(err) {
                    tableContainer.innerHTML = '<p style="color:red;">Failed to load holiday data: ' + err.message + '</p>';
                });
        }

        function renderTable(data, year) {
            if (!data || data.length === 0) {
                tableContainer.innerHTML = '<p style="color:#666;">No holiday data for year ' + year + '</p>';
                return;
            }

            var html = '<table class="holiday-table"><thead><tr><th>Date</th><th>Name</th><th>Type</th><th>Action</th></tr></thead><tbody>';
            data.forEach(function(item) {
                var typeClass = item.isHoliday ? 'type-holiday' : 'type-workday';
                var typeLabel = item.isHoliday ? 'Holiday' : 'Workday (Adjusted)';
                html += '<tr>' +
                    '<td>' + esc(item.date) + '</td>' +
                    '<td>' + esc(item.name || '-') + '</td>' +
                    '<td class="' + typeClass + '">' + typeLabel + '</td>' +
                    '<td><a href="#" class="delete-btn" data-date="' + esc(item.date) + '" title="Delete">Delete</a></td>' +
                    '</tr>';
            });
            html += '</tbody></table>';

            var holidayCount = data.filter(function(h) { return h.isHoliday; }).length;
            var workdayCount = data.filter(function(h) { return !h.isHoliday; }).length;
            html += '<p style="margin-top:8px;color:#666;font-size:12px;">';
            html += 'Total: ' + data.length + ' entries';
            html += ' (Holidays: ' + holidayCount + ', Adjusted Workdays: ' + workdayCount + ')';
            html += '</p>';

            tableContainer.innerHTML = html;

            tableContainer.querySelectorAll('.delete-btn').forEach(function(btn) {
                btn.addEventListener('click', function(e) {
                    e.preventDefault();
                    var date = btn.getAttribute('data-date');
                    if (confirm('Are you sure you want to delete the holiday entry for ' + date + '?')) {
                        postWithCrumb('deleteHoliday', { date: date });
                    }
                });
            });
        }

        function esc(str) {
            if (!str) return '';
            return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }
    });
})();
