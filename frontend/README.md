# TripForge Frontend

React 18 + Vite + Tailwind CSS frontend for TripForge.

## Stack
- React 18, React Router DOM 6
- Tailwind CSS 3 (dark navy + cyan theme)
- Axios with JWT interceptors
- Context API for auth state

## Run Locally

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open: http://localhost:5173

## Build

```bash
npm run build
npm run preview
```

## Pages

| Route | Page | Auth |
|---|---|---|
| `/login` | Login | Public |
| `/register` | Register | Public |
| `/dashboard` | Dashboard | Protected |
| `/trip/create` | Create Trip | Protected |
| `/trip/result` | Trip Result | Protected |
| `/trip/history` | Trip History | Protected |
| `/trip/:id` | Trip Details | Protected |
