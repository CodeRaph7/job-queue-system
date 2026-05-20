package com.jobqueue.service;

import com.jobqueue.model.Role;
import com.jobqueue.model.User;

public class PermissionManager {
    
    public enum Permission {
        CREATE_JOB,
        DELETE_JOB,
        CANCEL_JOB,
        CHANGE_JOB_STATUS,
        VIEW_ALL_JOBS,
        MANAGE_USERS,
        VIEW_STATISTICS
    }
    
    public static boolean hasPermission(User user, Permission permission) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        
        Role role = user.getRole();
        
        switch (role) {
            case ADMIN:
                // ADMIN has all permissions
                return true;

            case USER:
                // USER can view jobs, statistics, and change job status
                switch (permission) {
                    case VIEW_ALL_JOBS:
                    case VIEW_STATISTICS:
                    case CHANGE_JOB_STATUS:
                        return true;
                    default:
                        return false;
                }

            default:
                return false;
        }
    }
    
    public static boolean canCreateJob(User user) {
        return hasPermission(user, Permission.CREATE_JOB);
    }
    
    public static boolean canDeleteJob(User user) {
        return hasPermission(user, Permission.DELETE_JOB);
    }
    
    public static boolean canCancelJob(User user) {
        return hasPermission(user, Permission.CANCEL_JOB);
    }
    
    public static boolean canManageUsers(User user) {
        return hasPermission(user, Permission.MANAGE_USERS);
    }
    
    public static boolean canChangeJobStatus(User user) {
        return hasPermission(user, Permission.CHANGE_JOB_STATUS);
    }
}
