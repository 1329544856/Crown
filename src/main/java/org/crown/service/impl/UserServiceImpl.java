package org.crown.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.collections4.CollectionUtils;
import org.crown.common.api.ApiAssert;
import org.crown.common.emuns.ErrorCodeEnum;
import org.crown.common.framework.service.impl.BaseServiceImpl;
import org.crown.common.kit.JWTTokenUtils;
import org.crown.emuns.StatusEnum;
import org.crown.mapper.UserMapper;
import org.crown.model.dto.TokenDTO;
import org.crown.model.dto.UserDetailsDTO;
import org.crown.model.entity.User;
import org.crown.model.entity.UserRole;
import org.crown.service.IResourceService;
import org.crown.service.IUserRoleService;
import org.crown.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * <p>
 * 系统用户表 服务实现类
 * </p>
 *
 * @author Caratacus
 * @since 2018-10-25
 */
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private IResourceService resourceService;
    @Autowired
    private IUserRoleService userRoleService;

    @Override
    @Transactional
    public User login(String loginName, String password, String ipAddr) {
        User user = getOne(Wrappers.<User>query().eq(User.LOGIN_NAME, loginName));
        //用户不存在
        ApiAssert.notNull(ErrorCodeEnum.USERNAME_OR_PASSWORD_IS_WRONG, user);
        //用户名密码错误
        ApiAssert.isTrue(ErrorCodeEnum.USERNAME_OR_PASSWORD_IS_WRONG, Md5Crypt.apr1Crypt(password, loginName).equals(user.getPassword()));
        //用户被禁用
        ApiAssert.isTrue(ErrorCodeEnum.USER_IS_DISABLED, StatusEnum.NORMAL.equals(user.getStatus()));
        user.setIp(ipAddr);
        updateById(user);
        return user;
    }

    @Override
    public TokenDTO getToken(User user) {
        Integer id = user.getId();
        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setUid(id);
        tokenDTO.setToken(JWTTokenUtils.generate(id, user.getEmail()));
        return tokenDTO;
    }

    @Override
    public UserDetailsDTO getUserDetails(Integer uid) {
        User user = getById(uid);
        ApiAssert.notNull(ErrorCodeEnum.USER_NOT_FOUND, user);
        UserDetailsDTO userDetails = user.convert(UserDetailsDTO.class);
        userDetails.setPerms(resourceService.getUserPerms(uid));
        return userDetails;
    }

    @Override
    @Transactional
    public void updatePassword(Integer uid, String oldPassword, String newPassword) {
        User user = getById(uid);
        ApiAssert.notNull(ErrorCodeEnum.USER_NOT_FOUND, user);
        //用户名密码错误
        ApiAssert.isTrue(ErrorCodeEnum.ORIGINAL_PASSWORD_IS_INCORRECT, Md5Crypt.apr1Crypt(oldPassword, user.getLoginName()).equals(user.getPassword()));
        user.setPassword(Md5Crypt.apr1Crypt(newPassword, user.getLoginName()));
        updateById(user);
    }

    @Override
    @Transactional
    public void resetPwd(Integer uid) {
        User user = getById(uid);
        ApiAssert.notNull(ErrorCodeEnum.USER_NOT_FOUND, user);
        user.setPassword(Md5Crypt.apr1Crypt(user.getLoginName(), user.getLoginName()));
        updateById(user);
    }

    @Override
    @Transactional
    public void updateStatus(Integer uid, StatusEnum status) {
        User user = getById(uid);
        ApiAssert.notNull(ErrorCodeEnum.USER_NOT_FOUND, user);
        user.setStatus(status);
        updateById(user);
    }

    @Override
    @Transactional
    public void saveUserRoles(Integer uid, List<Integer> roleIds) {
        if (CollectionUtils.isNotEmpty(roleIds)) {
            userRoleService.remove(Wrappers.<UserRole>query().eq(UserRole.UID, uid));
            userRoleService.saveBatch(roleIds.stream().map(e -> {
                UserRole userRole = new UserRole();
                userRole.setRoleId(e);
                userRole.setUid(uid);
                return userRole;
            }).collect(Collectors.toList()));
        }
    }

}
