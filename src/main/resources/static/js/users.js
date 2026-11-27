const api = '/api';
const byId = id => document.getElementById(id);
let roles = [];

async function request(path, method = 'GET', body) {
    const headers = {Accept: 'application/json'};
    if (method !== 'GET') {
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        if (!csrfHeader?.content || !csrfToken?.content) {
            throw new Error('Security token missing. Reload the page and try again.');
        }
        headers[csrfHeader.content] = csrfToken.content;
        if (body) headers['Content-Type'] = 'application/json';
    }
    const response = await fetch(api + path, {
        method, headers, body: body ? JSON.stringify(body) : undefined
    });
    if (!response.ok || response.redirected) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Request failed: ' + response.status);
    }
    return response.status === 204 || response.status === 201 ? null : response.json();
}

function showError(error) {
    const alert = byId('error');
    alert.textContent = error.message;
    alert.classList.remove('d-none');
}

function row(user) {
    const tr = document.createElement('tr');
    [user.id, user.name, user.lastName, user.age, user.email, user.roleNames].forEach(value => {
        tr.insertCell().textContent = value ?? '';
    });
    return tr;
}

async function loadCurrent() {
    const user = await request('/user');
    byId('current-email').textContent = user.email;
    byId('current-roles').textContent = user.roleNames;
    if (byId('profile-body')) byId('profile-body').replaceChildren(row(user));
    if (byId('admin-link')) byId('admin-link').hidden = !user.roles.some(role => role.name === 'ROLE_ADMIN');
}

function roleOptions(form, selected = []) {
    form.querySelector('select[name="roleIds"]').replaceChildren(...roles.map(role => {
        const option = new Option(role.name.replace('ROLE_', ''), role.id);
        option.selected = selected.includes(role.id);
        return option;
    }));
}

async function openModal(id, action) {
    const user = await request('/users/' + id);
    const form = byId(action + '-form');
    form.reset();
    form.dataset.id = id;
    form.querySelectorAll('[data-field]').forEach(input => input.value = user[input.dataset.field] ?? '');
    roleOptions(form, user.roles.map(role => role.id));
    $('#' + action + '-modal').modal('show');
}

async function loadUsers() {
    const users = await request('/users');
    byId('users-body').replaceChildren(...users.map(user => {
        const tr = row(user);
        for (const action of ['edit', 'delete']) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'btn btn-sm btn-' + (action === 'edit' ? 'info' : 'danger');
            button.textContent = action === 'edit' ? 'Edit' : 'Delete';
            button.addEventListener('click', () => openModal(user.id, action).catch(showError));
            tr.insertCell().append(button);
        }
        return tr;
    }));
}

function formData(form) {
    const data = new FormData(form);
    return {
        name: data.get('name'), lastName: data.get('lastName'),
        age: data.get('age') === '' ? null : Number(data.get('age')), email: data.get('email'),
        password: data.get('password'), roleIds: data.getAll('roleIds').map(Number)
    };
}

function bindForm(action, method) {
    const form = byId(action + '-form');
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const button = form.querySelector('[type="submit"]');
        button.disabled = true;
        byId('error').classList.add('d-none');
        try {
            const path = '/users' + (action === 'create' ? '' : '/' + form.dataset.id);
            await request(path, method, action === 'delete' ? undefined : formData(form));
            if (action === 'create') {
                form.reset();
                roleOptions(form, roles.filter(role => role.name === 'ROLE_USER').map(role => role.id));
                $('a[href="#users-table"]').tab('show');
            } else {
                $('#' + action + '-modal').modal('hide');
            }
            await loadUsers();
            await loadCurrent();
        } catch (error) {
            showError(error);
        } finally {
            button.disabled = false;
        }
    });
}

async function init() {
    await loadCurrent();
    if (byId('users-body')) {
        roles = await request('/roles');
        roleOptions(byId('create-form'), roles.filter(role => role.name === 'ROLE_USER').map(role => role.id));
        bindForm('create', 'POST');
        bindForm('edit', 'PUT');
        bindForm('delete', 'DELETE');
        await loadUsers();
    }
}
init().catch(showError);
