DO $$
BEGIN
    IF '${seed_demo_users}' <> 'true' THEN
        DELETE FROM orders
        WHERE user_id IN ('demo@scalemart.dev', 'admin@scalemart.dev');

        DELETE FROM wishlists
        WHERE user_id IN ('demo@scalemart.dev', 'admin@scalemart.dev');

        DELETE FROM reviews
        WHERE user_id IN ('demo@scalemart.dev', 'admin@scalemart.dev');

        DELETE FROM admin_audit_logs
        WHERE actor_username IN ('demo@scalemart.dev', 'admin@scalemart.dev')
           OR target_username IN ('demo@scalemart.dev', 'admin@scalemart.dev');

        DELETE FROM app_users
        WHERE email IN ('demo@scalemart.dev', 'admin@scalemart.dev');

        DELETE FROM users
        WHERE username IN ('demo@scalemart.dev', 'admin@scalemart.dev');
    END IF;
END $$;
